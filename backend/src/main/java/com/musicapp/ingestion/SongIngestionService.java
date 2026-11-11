package com.musicapp.ingestion;

import com.musicapp.ingestion.provider.MusicProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pulls the latest songs from every enabled provider and publishes them to Kafka.
 * Persistence happens asynchronously in {@link SongEventConsumer}.
 */
@Service
public class SongIngestionService {

    private static final Logger log = LoggerFactory.getLogger(SongIngestionService.class);
    private static final long PUBLISH_TIMEOUT_SECONDS = 60;

    private final List<MusicProvider> providers;
    private final SongEventProducer producer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SongIngestionService(List<MusicProvider> providers, SongEventProducer producer) {
        this.providers = providers;
        this.producer = producer;
    }

    public IngestionReport ingest() {
        if (!running.compareAndSet(false, true)) {
            log.info("Ingestion already running, skipping this trigger");
            return IngestionReport.skippedRun();
        }
        try {
            return doIngest();
        } finally {
            running.set(false);
        }
    }

    private IngestionReport doIngest() {
        Map<String, SongEvent> unique = new LinkedHashMap<>();
        List<String> failed = new ArrayList<>();

        for (MusicProvider provider : providers) {
            try {
                List<SongEvent> events = provider.fetchLatest();
                events.forEach(e -> unique.putIfAbsent(e.key(), e));
                log.info("Provider {} returned {} songs", provider.name(), events.size());
            } catch (Exception e) {
                // One provider being down must not stop the others.
                log.warn("Provider {} failed: {}", provider.name(), e.getMessage());
                failed.add(provider.name());
            }
        }

        List<CompletableFuture<?>> sends = unique.values().stream()
                .<CompletableFuture<?>>map(producer::publish)
                .toList();

        int published = 0;
        for (CompletableFuture<?> send : sends) {
            try {
                send.get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                published++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // already logged by the producer
            }
        }
        log.info("Ingestion finished: fetched={}, published={}, failedProviders={}", unique.size(), published, failed);
        return new IngestionReport(false, unique.size(), published, List.copyOf(failed));
    }
}
