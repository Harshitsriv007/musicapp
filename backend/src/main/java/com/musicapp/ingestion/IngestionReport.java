package com.musicapp.ingestion;

import java.util.List;

/**
 * Outcome of one ingestion run.
 *
 * @param skipped         true when another run was already in progress
 * @param published       events successfully written to Kafka
 * @param failedProviders providers whose fetch failed in this run
 */
public record IngestionReport(boolean skipped, int fetched, int published, List<String> failedProviders) {

    public static IngestionReport skippedRun() {
        return new IngestionReport(true, 0, 0, List.of());
    }
}
