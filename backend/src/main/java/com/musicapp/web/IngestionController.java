package com.musicapp.web;

import com.musicapp.ingestion.SongIngestionService;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets an operator (or the UI "refresh" button) trigger the cron job immediately. */
@RestController
@RequestMapping("/api/admin/ingestion")
public class IngestionController {

    private final SongIngestionService ingestionService;
    private final Executor executor;

    public IngestionController(SongIngestionService ingestionService,
                               @Qualifier("applicationTaskExecutor") Executor executor) {
        this.ingestionService = ingestionService;
        this.executor = executor;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> run() {
        executor.execute(ingestionService::ingest);
        return ResponseEntity.accepted().body(Map.of("status", "started"));
    }
}
