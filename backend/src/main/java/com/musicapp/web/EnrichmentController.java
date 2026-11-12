package com.musicapp.web;

import com.musicapp.enrichment.ReleaseYearEnrichmentService;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Runs the release-year lookup now instead of waiting for the next scheduled batch. */
@RestController
public class EnrichmentController {

    private final ReleaseYearEnrichmentService enrichment;
    private final Executor executor;

    public EnrichmentController(ReleaseYearEnrichmentService enrichment,
                                @Qualifier("applicationTaskExecutor") Executor executor) {
        this.enrichment = enrichment;
        this.executor = executor;
    }

    @PostMapping("/api/admin/enrichment/run")
    public ResponseEntity<Map<String, String>> run() {
        executor.execute(enrichment::enrichPending);
        return ResponseEntity.accepted().body(Map.of("status", "started"));
    }
}
