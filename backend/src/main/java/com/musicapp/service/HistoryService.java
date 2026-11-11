package com.musicapp.service;

import com.musicapp.domain.PlayHistory;
import com.musicapp.download.SongPlayedEvent;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.PlayHistoryRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.web.dto.HistoryEntryDto;
import com.musicapp.web.dto.PageResponse;
import com.musicapp.web.dto.SongDto;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

    private final PlayHistoryRepository history;
    private final SongRepository songs;
    private final LikedSongRepository likes;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public HistoryService(PlayHistoryRepository history, SongRepository songs, LikedSongRepository likes, Clock clock,
                          ApplicationEventPublisher events) {
        this.history = history;
        this.songs = songs;
        this.likes = likes;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public HistoryEntryDto recordPlay(String userId, Long songId) {
        var song = songs.findById(songId).orElseThrow(() -> new SongNotFoundException(songId));
        var saved = history.save(new PlayHistory(userId, song, Instant.now(clock)));
        events.publishEvent(new SongPlayedEvent(songId)); // may cache the audio on first play
        return new HistoryEntryDto(saved.getId(), SongDto.from(song, likes.existsByUserIdAndSongId(userId, songId)),
                saved.getPlayedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoryEntryDto> list(String userId, int page, int size) {
        return PageResponse.of(history.findByUser(userId, PageRequest.of(page, size)), entries -> {
            Set<Long> liked = entries.isEmpty() ? Set.of()
                    : likes.findLikedSongIds(userId, entries.stream().map(h -> h.getSong().getId()).toList());
            return entries.stream()
                    .map(h -> new HistoryEntryDto(h.getId(), SongDto.from(h.getSong(), liked.contains(h.getSong().getId())),
                            h.getPlayedAt()))
                    .toList();
        });
    }

    @Transactional
    public void clear(String userId) {
        history.deleteByUser(userId);
    }
}
