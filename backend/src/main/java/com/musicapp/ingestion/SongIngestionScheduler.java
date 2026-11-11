package com.musicapp.ingestion;

import com.musicapp.config.AppProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cron trigger for {@link SongIngestionService}. */
@Component
public class SongIngestionScheduler {

    private final SongIngestionService ingestionService;
    private final AppProperties props;

    public SongIngestionScheduler(SongIngestionService ingestionService, AppProperties props) {
        this.ingestionService = ingestionService;
        this.props = props;
    }

    @Scheduled(cron = "${musicapp.ingestion.cron}")
    public void scheduledRun() {
        ingestionService.ingest();
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        if (props.ingestion().runOnStartup()) {
            ingestionService.ingest();
        }
    }
}
