package ru.hivetech.telemetry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class TelemetryApiClient {
    private static final Gson GSON = new Gson();

    private final TelemetryConfig config;
    private volatile int lastStatusCode;

    public TelemetryApiClient(TelemetryConfig config) {
        this.config = config;
    }

    public BatchResponse events(List<TelemetryEvent> events)
        throws IOException {
        JsonObject body = new JsonObject();
        JsonArray rows = new JsonArray();

        for (TelemetryEvent event : events) {
            rows.add(GSON.toJsonTree(event));
        }

        body.add("events", rows);

        JsonObject response = request(
            "/telemetry/events",
            GSON.toJson(body)
        );

        BatchResponse result = new BatchResponse();
        result.accepted = intValue(response, "accepted");
        result.duplicates = intValue(response, "duplicates");
        result.unresolvedPlayers =
            intValue(response, "unresolved_players");

        return result;
    }

    public void heartbeat(
        int onlinePlayers,
        int queueSize,
        int bufferedEvents,
        String lastError
    ) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty(
            "telemetry_version",
            HiveTechTelemetry.VERSION
        );
        body.addProperty("minecraft_version", "1.12.2");
        body.addProperty(
            "forge_version",
            net.minecraftforge.common.ForgeVersion.getVersion()
        );
        body.addProperty(
            "online_players",
            Math.max(0, onlinePlayers)
        );
        body.addProperty(
            "queue_size",
            Math.max(0, queueSize)
        );
        body.addProperty(
            "buffered_events",
            Math.max(0, bufferedEvents)
        );
        body.addProperty(
            "last_error",
            lastError == null ? "" : lastError
        );

        request(
            "/telemetry/heartbeat",
            GSON.toJson(body)
        );
    }

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    private JsonObject request(
        String path,
        String body
    ) throws IOException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(config.getApiUrl() + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(
                config.getConnectTimeoutSeconds() * 1000
            );
            connection.setReadTimeout(
                config.getReadTimeoutSeconds() * 1000
            );
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestProperty(
                "Accept",
                "application/json"
            );
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            );
            connection.setRequestProperty(
                "User-Agent",
                "HiveTechTelemetry/" + HiveTechTelemetry.VERSION
            );
            connection.setRequestProperty(
                "Authorization",
                "Bearer " + config.getToken()
            );
            connection.setRequestProperty(
                "X-HiveTech-Server",
                config.getServerCode()
            );

            byte[] payload =
                body.getBytes(StandardCharsets.UTF_8);

            connection.setFixedLengthStreamingMode(
                payload.length
            );

            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
            }

            int status = connection.getResponseCode();
            lastStatusCode = status;

            InputStream stream =
                status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response =
                readLimited(stream, 2 * 1024 * 1024);

            if (status < 200 || status >= 300) {
                throw new ApiException(
                    status,
                    sanitizeError(response)
                );
            }

            if (response.trim().isEmpty()) {
                throw new IOException(
                    "Empty telemetry API response"
                );
            }

            try {
                return new JsonParser()
                    .parse(response)
                    .getAsJsonObject();
            } catch (RuntimeException e) {
                throw new IOException(
                    "Invalid telemetry API JSON",
                    e
                );
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static int intValue(
        JsonObject object,
        String key
    ) {
        return object.has(key)
            ? object.get(key).getAsInt()
            : 0;
    }

    private static String readLimited(
        InputStream stream,
        int maxBytes
    ) throws IOException {
        if (stream == null) {
            return "";
        }

        ByteArrayOutputStream out =
            new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int total = 0;
        int read;

        while ((read = stream.read(buffer)) != -1) {
            if (total + read > maxBytes) {
                int allowed = maxBytes - total;
                if (allowed > 0) {
                    out.write(buffer, 0, allowed);
                }
                break;
            }

            out.write(buffer, 0, read);
            total += read;
        }

        return new String(
            out.toByteArray(),
            StandardCharsets.UTF_8
        );
    }

    private static String sanitizeError(String response) {
        if (response == null) {
            return "";
        }

        String clean =
            response.replaceAll("[\\r\\n]+", " ").trim();

        return clean.length() > 500
            ? clean.substring(0, 500)
            : clean;
    }

    public static final class BatchResponse {
        private int accepted;
        private int duplicates;
        private int unresolvedPlayers;

        public int getAccepted() { return accepted; }
        public int getDuplicates() { return duplicates; }
        public int getUnresolvedPlayers() {
            return unresolvedPlayers;
        }
    }

    public static final class ApiException
        extends IOException {
        private final int statusCode;

        public ApiException(
            int statusCode,
            String message
        ) {
            super(
                "HTTP "
                + statusCode
                + (
                    message == null || message.isEmpty()
                        ? ""
                        : ": " + message
                )
            );
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
