package dev.proxyhistorycolorer.sync;

import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.organizer.OrganizerItem;
import dev.proxyhistorycolorer.model.EndpointAnnotation;
import dev.proxyhistorycolorer.model.EndpointKey;
import dev.proxyhistorycolorer.store.EndpointRegistry;
import dev.proxyhistorycolorer.store.ProjectStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OrganizerSynchronizer implements AutoCloseable {
    private static final long SYNC_INTERVAL_SECONDS = 2;

    private final Organizer organizer;
    private final EndpointRegistry registry;
    private final ProjectStore projectStore;
    private final Logging logging;
    private final ScheduledExecutorService executor;

    public OrganizerSynchronizer(
            Organizer organizer,
            EndpointRegistry registry,
            ProjectStore projectStore,
            Logging logging
    ) {
        this.organizer = Objects.requireNonNull(organizer, "organizer");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.projectStore = Objects.requireNonNull(projectStore, "projectStore");
        this.logging = Objects.requireNonNull(logging, "logging");
        this.executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "proxy-history-colorer-organizer-sync");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        executor.scheduleWithFixedDelay(
                this::syncSafely,
                0,
                SYNC_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    void sync() {
        Map<EndpointKey, EndpointAnnotation> current = buildSnapshot(organizer.items());
        if (registry.replace(current)) {
            projectStore.save(current);
            logging.logToOutput("Synchronized " + current.size() + " commented endpoints from Organizer");
        }
    }

    static Map<EndpointKey, EndpointAnnotation> buildSnapshot(List<OrganizerItem> items) {
        Map<EndpointKey, EndpointAnnotation> current = new HashMap<>();
        Map<EndpointKey, OrganizerItem> selectedItems = new HashMap<>();
        for (OrganizerItem item : items) {
            if (!item.annotations().hasNotes()) {
                continue;
            }

            String notes = item.annotations().notes();
            if (notes == null || notes.isBlank()) {
                continue;
            }

            EndpointKey key = EndpointKey.from(item.request());
            HighlightColor color = item.annotations().hasHighlightColor()
                    ? item.annotations().highlightColor()
                    : HighlightColor.GRAY;
            EndpointAnnotation annotation = new EndpointAnnotation(notes, color, item.id());
            EndpointAnnotation selected = current.get(key);
            if (selected == null) {
                current.put(key, annotation);
                selectedItems.put(key, item);
            } else if (annotation.organizerItemId() > selected.organizerItemId()) {
                markAsSuperseded(selectedItems.get(key));
                current.put(key, annotation);
                selectedItems.put(key, item);
            } else {
                markAsSuperseded(item);
            }
        }
        return Map.copyOf(current);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void syncSafely() {
        try {
            sync();
        } catch (RuntimeException exception) {
            logging.logToError("Failed to synchronize Organizer: " + exception.getMessage());
        }
    }

    private static void markAsSuperseded(OrganizerItem item) {
        if (!item.annotations().hasHighlightColor()
                || item.annotations().highlightColor() != HighlightColor.PINK) {
            item.annotations().setHighlightColor(HighlightColor.PINK);
        }
    }
}
