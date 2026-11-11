package com.musicapp.service;

import com.musicapp.web.dto.SongDto;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Server-Sent Events hub: pushes every newly ingested song to all open browser/mobile clients. */
@Component
public class SongUpdatesBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SongUpdatesBroadcaster.class);
    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        send(emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    public void songAdded(SongDto song) {
        emitters.forEach(e -> send(e, SseEmitter.event().name("song-added").id(String.valueOf(song.id())).data(song)));
    }

    /** Keeps idle connections alive through proxies (nginx, mobile carriers). */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.forEach(e -> send(e, SseEmitter.event().comment("ping")));
    }

    public int subscriberCount() {
        return emitters.size();
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            log.debug("Dropping SSE subscriber: {}", e.getMessage());
            emitters.remove(emitter);
            emitter.completeWithError(e);
        }
    }
}
