package com.musicapp.web;

import com.musicapp.service.SongService;
import com.musicapp.service.SongUpdatesBroadcaster;
import com.musicapp.web.dto.PageResponse;
import com.musicapp.web.dto.SongDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/songs")
@Validated
public class SongController {

    private final SongService songService;
    private final SongUpdatesBroadcaster broadcaster;

    public SongController(SongService songService, SongUpdatesBroadcaster broadcaster) {
        this.songService = songService;
        this.broadcaster = broadcaster;
    }

    @GetMapping
    public PageResponse<SongDto> list(
            @RequestHeader(name = UserIds.HEADER, defaultValue = UserIds.DEFAULT) String userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size) {
        return songService.list(UserIds.require(userId), q, page, size);
    }

    @GetMapping("/new")
    public List<SongDto> newest(
            @RequestHeader(name = UserIds.HEADER, defaultValue = UserIds.DEFAULT) String userId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return songService.newest(UserIds.require(userId), limit);
    }

    @GetMapping("/{id}")
    public SongDto get(
            @RequestHeader(name = UserIds.HEADER, defaultValue = UserIds.DEFAULT) String userId,
            @PathVariable Long id) {
        return songService.get(UserIds.require(userId), id);
    }

    /** Live feed of newly added songs (Server-Sent Events). */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return broadcaster.subscribe();
    }
}
