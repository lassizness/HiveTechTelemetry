package ru.hivetech.telemetry;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class TelemetryBridge {
    private final TelemetryConfig config;
    private final TelemetryQueue queue;
    private final TelemetryCollector collector;
    private final TelemetryApiClient api;

    private ScheduledExecutorService executor;

    private volatile boolean started;
    private volatile boolean forceFlush;
    private volatile long nextBatchAt;
    private volatile long nextBufferFlushAt;
    private volatile long nextHeartbeatAt;
    private volatile long retryAfter;

    private volatile int consecutiveFailures;
    private volatile int lastHttpStatus;
    private volatile String lastError = "";
    private volatile long lastSuccessAt;
    private volatile long lastHeartbeatAt;
    private volatile long sentEvents;
    private volatile long duplicateEvents;
    private volatile long unresolvedPlayers;

    public TelemetryBridge(
        TelemetryConfig config,
        TelemetryQueue queue,
        TelemetryCollector collector
    ) {
        this.config = config;
        this.queue = queue;
        this.collector = collector;
        this.api = new TelemetryApiClient(config);
    }

    public synchronized void start() {
        if (started) {
            return;
        }

        if (!config.isEnabled()) {
            HiveTechTelemetry.LOG.info(
                "HiveTech Telemetry is disabled in config."
            );
            return;
        }

        if (!config.hasCredentials()) {
            HiveTechTelemetry.LOG.error(
                "HiveTech Telemetry is enabled but API credentials are not configured."
            );
            return;
        }

        started = true;
        long now = System.currentTimeMillis();
        nextBatchAt = now;
        nextBufferFlushAt = now;
        nextHeartbeatAt = now;

        executor = Executors.newSingleThreadScheduledExecutor(
            new TelemetryThreadFactory()
        );

        executor.scheduleWithFixedDelay(
            this::tickSafely,
            0L,
            1L,
            TimeUnit.SECONDS
        );

        HiveTechTelemetry.LOG.info(
            "HiveTech Telemetry started: api={}, serverCode={}, queuedEvents={}, integrations={}",
            config.getApiUrl(),
            config.getServerCode(),
            queue.size(),
            TelemetryCompat.detectedIntegrations()
        );
    }

    public synchronized void stop() {
        if (!started) {
            collector.flushCounters();
            queue.persist();
            return;
        }

        started = false;

        collector.flushCounters();
        queue.persist();

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        HiveTechTelemetry.LOG.info(
            "HiveTech Telemetry stopped. queuedEvents={}, sentEvents={}, duplicates={}",
            queue.size(),
            sentEvents,
            duplicateEvents
        );
    }

    public void requestFlush() {
        forceFlush = true;
        retryAfter = 0L;
        nextBatchAt = 0L;
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Throwable e) {
            recordFailure(e);
            HiveTechTelemetry.LOG.error(
                "Unhandled telemetry worker error",
                e
            );
        }
    }

    private void tick() {
        if (!started) {
            return;
        }

        long now = System.currentTimeMillis();

        if (
            forceFlush
            || now >= nextBufferFlushAt
        ) {
            collector.flushCounters();
            queue.persistIfDirty();

            nextBufferFlushAt =
                now
                + config.getBatchIntervalSeconds() * 1000L;
        }

        if (now < retryAfter) {
            return;
        }

        if (
            queue.size() > 0
            && (
                forceFlush
                || now >= nextBatchAt
            )
        ) {
            forceFlush = false;
            sendBatch();

            now = System.currentTimeMillis();

            if (now < retryAfter) {
                return;
            }
        }

        if (now >= nextHeartbeatAt) {
            sendHeartbeat();
        }
    }

    private void sendBatch() {
        List<TelemetryEvent> batch =
            queue.snapshot(config.getMaxBatchSize());

        if (batch.isEmpty()) {
            nextBatchAt =
                System.currentTimeMillis()
                + config.getBatchIntervalSeconds() * 1000L;
            return;
        }

        try {
            TelemetryApiClient.BatchResponse response =
                api.events(batch);

            lastHttpStatus = api.getLastStatusCode();

            int accounted =
                response.getAccepted()
                + response.getDuplicates();

            if (accounted != batch.size()) {
                throw new IllegalStateException(
                    "Telemetry API acknowledged "
                    + accounted
                    + " of "
                    + batch.size()
                    + " events"
                );
            }

            queue.removeFirst(batch.size());
            queue.persistIfDirty();

            sentEvents += response.getAccepted();
            duplicateEvents += response.getDuplicates();
            unresolvedPlayers +=
                response.getUnresolvedPlayers();

            clearFailure();

            nextBatchAt =
                System.currentTimeMillis()
                + config.getBatchIntervalSeconds() * 1000L;
        } catch (Throwable e) {
            lastHttpStatus = api.getLastStatusCode();
            recordFailure(e);
        }
    }

    private void sendHeartbeat() {
        try {
            api.heartbeat(
                collector.onlinePlayers(),
                queue.size(),
                collector.bufferedCounters(),
                lastError
            );

            lastHttpStatus = api.getLastStatusCode();
            lastHeartbeatAt = System.currentTimeMillis();
            clearFailure();

            nextHeartbeatAt =
                System.currentTimeMillis()
                + config.getHeartbeatIntervalSeconds() * 1000L;
        } catch (Throwable e) {
            lastHttpStatus = api.getLastStatusCode();
            recordFailure(e);
        }
    }

    private void clearFailure() {
        consecutiveFailures = 0;
        retryAfter = 0L;
        lastError = "";
        lastSuccessAt = System.currentTimeMillis();
    }

    private void recordFailure(Throwable error) {
        consecutiveFailures++;

        long multiplier =
            1L << Math.min(10, Math.max(0, consecutiveFailures - 1));

        long delaySeconds = Math.min(
            config.getRetryMaxSeconds(),
            config.getRetryBaseSeconds() * multiplier
        );

        retryAfter =
            System.currentTimeMillis()
            + delaySeconds * 1000L;

        String message =
            error == null || error.getMessage() == null
                ? error == null
                    ? "Unknown telemetry error"
                    : error.getClass().getSimpleName()
                : error.getMessage();

        message = message
            .replace('\r', ' ')
            .replace('\n', ' ')
            .trim();

        lastError =
            message.length() > 500
                ? message.substring(0, 500)
                : message;

        HiveTechTelemetry.LOG.warn(
            "Telemetry API error (attempt {}, retry in {}s): {}",
            consecutiveFailures,
            delaySeconds,
            lastError
        );
    }

    public boolean isStarted() {
        return started;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getBufferedCounters() {
        return collector.bufferedCounters();
    }

    public int getOnlinePlayers() {
        return collector.onlinePlayers();
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public int getLastHttpStatus() {
        return lastHttpStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public long getLastSuccessAt() {
        return lastSuccessAt;
    }

    public long getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public long getSentEvents() {
        return sentEvents;
    }

    public long getDuplicateEvents() {
        return duplicateEvents;
    }

    public long getUnresolvedPlayers() {
        return unresolvedPlayers;
    }

    private static final class TelemetryThreadFactory
        implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(
                runnable,
                "HiveTechTelemetry-Worker"
            );
            thread.setDaemon(true);
            return thread;
        }
    }
}
