package ru.hivetech.telemetry;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class TelemetryCollector {
    private final TelemetryQueue queue;
    private final TelemetryConfig config;

    private final ConcurrentHashMap<String, String> sessions =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> playtimeTicks =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PendingCounter> counters =
        new ConcurrentHashMap<>();

    private volatile long lastQueueFullWarningAt;

    public TelemetryCollector(
        TelemetryQueue queue,
        TelemetryConfig config
    ) {
        this.queue = queue;
        this.config = config;
    }

    public void playerLogin(EntityPlayerMP player) {
        String key = playerKey(player);
        String sessionId = UUID.randomUUID().toString();

        sessions.put(key, sessionId);
        playtimeTicks.put(key, new AtomicInteger());

        emitImmediate(
            player,
            sessionId,
            "player.login",
            "forge",
            null,
            1L,
            playerMetadata(player)
        );
    }

    public void playerLogout(EntityPlayerMP player) {
        String key = playerKey(player);
        String sessionId = sessions.get(key);

        emitImmediate(
            player,
            sessionId,
            "player.logout",
            "forge",
            null,
            1L,
            playerMetadata(player)
        );

        sessions.remove(key);
        playtimeTicks.remove(key);
    }

    public void playerTick(EntityPlayerMP player) {
        String key = playerKey(player);

        sessions.computeIfAbsent(
            key,
            ignored -> UUID.randomUUID().toString()
        );

        AtomicInteger ticks = playtimeTicks.computeIfAbsent(
            key,
            ignored -> new AtomicInteger()
        );

        int threshold = Math.max(
            1,
            config.getPlaytimeIntervalSeconds() * 20
        );

        int current = ticks.incrementAndGet();

        if (
            current >= threshold
            && ticks.compareAndSet(current, 0)
        ) {
            emitCounter(
                player,
                "player.playtime",
                "forge",
                null,
                config.getPlaytimeIntervalSeconds(),
                null
            );
        }
    }

    public void emitImmediate(
        EntityPlayerMP player,
        String type,
        String source,
        String object,
        long amount,
        JsonObject metadata
    ) {
        emitImmediate(
            player,
            currentSession(player),
            type,
            source,
            object,
            amount,
            metadata
        );
    }

    private void emitImmediate(
        EntityPlayerMP player,
        String sessionId,
        String type,
        String source,
        String object,
        long amount,
        JsonObject metadata
    ) {
        if (!config.isEnabled()) {
            return;
        }

        TelemetryEvent event = TelemetryEvent.create(
            sessionId,
            player.getUniqueID().toString(),
            player.getName(),
            type,
            source,
            object,
            amount,
            System.currentTimeMillis(),
            metadata
        );

        if (!queue.enqueue(event)) {
            warnQueueFull();
        }
    }

    public void emitCounter(
        EntityPlayerMP player,
        String type,
        String source,
        String object,
        long amount,
        JsonObject metadata
    ) {
        if (!config.isEnabled() || amount <= 0L) {
            return;
        }

        String sessionId = currentSession(player);
        String metadataJson = metadata == null ? "" : metadata.toString();

        String key =
            player.getUniqueID().toString()
            + "\u0001" + sessionId
            + "\u0001" + type
            + "\u0001" + source
            + "\u0001" + (object == null ? "" : object)
            + "\u0001" + metadataJson;

        PendingCounter pending = counters.computeIfAbsent(
            key,
            ignored -> new PendingCounter(
                sessionId,
                player.getUniqueID().toString(),
                player.getName(),
                type,
                source,
                object,
                metadata,
                System.currentTimeMillis()
            )
        );

        pending.amount.addAndGet(amount);
    }

    public int flushCounters() {
        int queued = 0;

        for (Map.Entry<String, PendingCounter> entry : counters.entrySet()) {
            PendingCounter pending = entry.getValue();
            long amount = pending.amount.getAndSet(0L);

            if (amount <= 0L) {
                counters.remove(entry.getKey(), pending);
                continue;
            }

            long emitted = Math.min(1000000000L, amount);

            TelemetryEvent event = TelemetryEvent.create(
                pending.sessionId,
                pending.playerUuid,
                pending.username,
                pending.type,
                pending.source,
                pending.object,
                emitted,
                pending.occurredAtMillis,
                pending.metadata
            );

            if (queue.enqueue(event)) {
                queued++;

                long remainder = amount - emitted;
                if (remainder > 0L) {
                    pending.amount.addAndGet(remainder);
                }

                if (pending.amount.get() == 0L) {
                    counters.remove(entry.getKey(), pending);
                }
            } else {
                pending.amount.addAndGet(amount);
                warnQueueFull();
                break;
            }
        }

        return queued;
    }

    public int bufferedCounters() {
        return counters.size();
    }

    public int onlinePlayers() {
        return sessions.size();
    }

    public String currentSession(EntityPlayerMP player) {
        return sessions.computeIfAbsent(
            playerKey(player),
            ignored -> UUID.randomUUID().toString()
        );
    }

    public JsonObject playerMetadata(EntityPlayerMP player) {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("dimension", player.dimension);
        return metadata;
    }

    private static String playerKey(EntityPlayerMP player) {
        return player.getUniqueID().toString();
    }

    private void warnQueueFull() {
        long now = System.currentTimeMillis();

        if (now - lastQueueFullWarningAt < 60000L) {
            return;
        }

        lastQueueFullWarningAt = now;

        HiveTechTelemetry.LOG.error(
            "Telemetry queue is full ({} events). New telemetry is being retained in counters where possible.",
            queue.size()
        );
    }

    private static final class PendingCounter {
        private final String sessionId;
        private final String playerUuid;
        private final String username;
        private final String type;
        private final String source;
        private final String object;
        private final JsonObject metadata;
        private final long occurredAtMillis;
        private final AtomicLong amount = new AtomicLong();

        private PendingCounter(
            String sessionId,
            String playerUuid,
            String username,
            String type,
            String source,
            String object,
            JsonObject metadata,
            long occurredAtMillis
        ) {
            this.sessionId = sessionId;
            this.playerUuid = playerUuid;
            this.username = username;
            this.type = type;
            this.source = source;
            this.object = object;
            this.metadata = metadata;
            this.occurredAtMillis = occurredAtMillis;
        }
    }
}
