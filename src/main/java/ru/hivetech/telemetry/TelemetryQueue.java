package ru.hivetech.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TelemetryQueue {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type LIST_TYPE =
        new TypeToken<List<TelemetryEvent>>() { }.getType();

    private final File file;
    private final int maxEvents;
    private final List<TelemetryEvent> events = new ArrayList<>();
    private boolean dirty;

    public TelemetryQueue(File file, int maxEvents) {
        this.file = file;
        this.maxEvents = Math.max(1000, maxEvents);
    }

    public synchronized void load() {
        events.clear();

        if (!file.isFile()) {
            dirty = false;
            return;
        }

        try (Reader reader = new InputStreamReader(
            new FileInputStream(file),
            StandardCharsets.UTF_8
        )) {
            List<TelemetryEvent> loaded = GSON.fromJson(reader, LIST_TYPE);
            if (loaded != null) {
                int from = Math.max(0, loaded.size() - maxEvents);
                events.addAll(loaded.subList(from, loaded.size()));
            }
            dirty = false;
        } catch (Exception e) {
            HiveTechTelemetry.LOG.error(
                "Failed to load telemetry queue from {}",
                file,
                e
            );
        }
    }

    public synchronized boolean enqueue(TelemetryEvent event) {
        if (event == null || events.size() >= maxEvents) {
            return false;
        }
        events.add(event);
        dirty = true;
        return true;
    }

    public synchronized List<TelemetryEvent> snapshot(int max) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        int size = Math.min(Math.max(1, max), events.size());
        return new ArrayList<>(events.subList(0, size));
    }

    public synchronized void removeFirst(int count) {
        if (count <= 0 || events.isEmpty()) {
            return;
        }

        int end = Math.min(count, events.size());
        events.subList(0, end).clear();
        dirty = true;
    }

    public synchronized int size() {
        return events.size();
    }

    public synchronized int remainingCapacity() {
        return Math.max(0, maxEvents - events.size());
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void persistIfDirty() {
        if (dirty) {
            persist();
        }
    }

    public synchronized void persist() {
        Path target = file.toPath();
        Path parent = target.getParent();

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path temp = target.resolveSibling(target.getFileName() + ".tmp");

            try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(temp.toFile()),
                StandardCharsets.UTF_8
            )) {
                GSON.toJson(events, LIST_TYPE, writer);
            }

            try {
                Files.move(
                    temp,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                    temp,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }

            dirty = false;
        } catch (Exception e) {
            HiveTechTelemetry.LOG.error(
                "Failed to persist telemetry queue to {}",
                file,
                e
            );
        }
    }
}
