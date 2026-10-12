package ru.hivetech.telemetry;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.UUID;

public final class TelemetryEvent {
    @SerializedName("id")
    private String id;

    @SerializedName("session_id")
    private String sessionId;

    @SerializedName("player_uuid")
    private String playerUuid;

    @SerializedName("username")
    private String username;

    @SerializedName("type")
    private String type;

    @SerializedName("source")
    private String source;

    @SerializedName("object")
    private String object;

    @SerializedName("amount")
    private long amount;

    @SerializedName("occurred_at")
    private String occurredAt;

    @SerializedName("metadata")
    private JsonObject metadata;

    public TelemetryEvent() {
    }

    public static TelemetryEvent create(
        String sessionId,
        String playerUuid,
        String username,
        String type,
        String source,
        String object,
        long amount,
        long occurredAtMillis,
        JsonObject metadata
    ) {
        TelemetryEvent event = new TelemetryEvent();
        event.id = UUID.randomUUID().toString();
        event.sessionId = emptyToNull(sessionId);
        event.playerUuid = emptyToNull(playerUuid);
        event.username = emptyToNull(username);
        event.type = type;
        event.source = source == null || source.isEmpty() ? "forge" : source;
        event.object = emptyToNull(object);
        event.amount = Math.max(1L, amount);
        event.occurredAt = Instant.ofEpochMilli(occurredAtMillis).toString();
        event.metadata = metadata;
        return event;
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getId() { return id; }
    public long getAmount() { return amount; }
}
