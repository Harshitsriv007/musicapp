package com.musicapp.ingestion;

import com.musicapp.service.SongService;
import com.musicapp.service.SongUpdatesBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes song events, stores them in MySQL and pushes newly added songs to connected clients. */
@Component
public class SongEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SongEventConsumer.class);

    private final SongService songService;
    private final SongUpdatesBroadcaster broadcaster;

    public SongEventConsumer(SongService songService, SongUpdatesBroadcaster broadcaster) {
        this.songService = songService;
        this.broadcaster = broadcaster;
    }

    @KafkaListener(topics = "${musicapp.kafka.songs-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onSongEvent(SongEvent event) {
        event.validate();
        songService.upsert(event).ifPresent(created -> {
            log.info("New song added: {} - {}", created.title(), created.artist());
            broadcaster.songAdded(created);
        });
    }
}
