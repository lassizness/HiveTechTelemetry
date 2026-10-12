package ru.hivetech.telemetry;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class TelemetryConfig {
    private final File file;

    private boolean enabled;
    private String apiUrl;
    private String serverCode;
    private String token;
    private int batchIntervalSeconds;
    private int heartbeatIntervalSeconds;
    private int playtimeIntervalSeconds;
    private int maxBatchSize;
    private int maxQueueEvents;
    private int connectTimeoutSeconds;
    private int readTimeoutSeconds;
    private int retryBaseSeconds;
    private int retryMaxSeconds;

    public TelemetryConfig(File file) {
        this.file = file;
    }

    public void load() {
        Configuration cfg = new Configuration(file);
        cfg.load();

        enabled = cfg.getBoolean(
            "enabled",
            "telemetry",
            true,
            "Enable HiveTech Telemetry"
        );

        apiUrl = trimSlash(cfg.getString(
            "apiUrl",
            "api",
            "https://hivegrid.online/internal/game",
            "HiveTech internal game API base URL"
        ));

        serverCode = cfg.getString(
            "serverCode",
            "api",
            "HiveTech",
            "Server code from HiveTech admin"
        ).trim();

        token = cfg.getString(
            "token",
            "api",
            "",
            "Per-server Bridge token. Never commit this value."
        ).trim();

        batchIntervalSeconds = cfg.getInt(
            "batchIntervalSeconds",
            "timing",
            5,
            1,
            60,
            "How often buffered gameplay counters are queued and sent"
        );

        heartbeatIntervalSeconds = cfg.getInt(
            "heartbeatIntervalSeconds",
            "timing",
            60,
            15,
            600,
            "Telemetry heartbeat interval"
        );

        playtimeIntervalSeconds = cfg.getInt(
            "playtimeIntervalSeconds",
            "timing",
            60,
            10,
            300,
            "Player playtime counter emission interval"
        );

        maxBatchSize = cfg.getInt(
            "maxBatchSize",
            "queue",
            200,
            1,
            500,
            "Maximum events per HTTP batch"
        );

        maxQueueEvents = cfg.getInt(
            "maxQueueEvents",
            "queue",
            250000,
            1000,
            1000000,
            "Maximum persisted telemetry events"
        );

        connectTimeoutSeconds = cfg.getInt(
            "connectTimeoutSeconds",
            "http",
            3,
            1,
            30,
            "HTTP connect timeout"
        );

        readTimeoutSeconds = cfg.getInt(
            "readTimeoutSeconds",
            "http",
            8,
            1,
            60,
            "HTTP read timeout"
        );

        retryBaseSeconds = cfg.getInt(
            "retryBaseSeconds",
            "http",
            5,
            1,
            60,
            "Initial retry delay after an API/network error"
        );

        retryMaxSeconds = cfg.getInt(
            "retryMaxSeconds",
            "http",
            300,
            10,
            3600,
            "Maximum retry delay"
        );

        if (cfg.hasChanged()) {
            cfg.save();
        }
    }

    private static String trimSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public boolean isEnabled() { return enabled; }
    public String getApiUrl() { return apiUrl; }
    public String getServerCode() { return serverCode; }
    public String getToken() { return token; }
    public int getBatchIntervalSeconds() { return batchIntervalSeconds; }
    public int getHeartbeatIntervalSeconds() { return heartbeatIntervalSeconds; }
    public int getPlaytimeIntervalSeconds() { return playtimeIntervalSeconds; }
    public int getMaxBatchSize() { return maxBatchSize; }
    public int getMaxQueueEvents() { return maxQueueEvents; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public int getRetryBaseSeconds() { return retryBaseSeconds; }
    public int getRetryMaxSeconds() { return retryMaxSeconds; }

    public boolean hasCredentials() {
        return !apiUrl.isEmpty()
            && !serverCode.isEmpty()
            && token.startsWith("htb_")
            && token.length() >= 32;
    }
}
