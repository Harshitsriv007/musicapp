package com.musicapp.service;

import com.musicapp.domain.PlayHistory;
import com.musicapp.repository.PlayHistoryRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.web.dto.HistoryEntryDto;
import com.musicapp.web.dto.PageResponse;
import com.musicapp.web.dto.SongDto;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

    private final PlayHistoryRepository history;
    private final SongRepository songs;
    private final SongService songService;
    private final Clock clock;

    public HistoryService(PlayHistoryRepository history, SongRepository songs, SongService songService, Clock clock) {
        this.history = history;
        this.songs = songs;
        this.songService = songService;
        this.clock = clock;
    }

    @Transactional
    public HistoryEntryDto recordPlay(String userId, Long songId) {
        var song = songs.findById(songId).orElseThrow(() -> new SongNotFoundException(songId));
        var saved = history.save(new PlayHistory(userId, song, Instant.now(clock)));
        return new HistoryEntryDto(saved.getId(), songService.toDtos(userId, List.of(song)).getFirst(),
                saved.getPlayedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoryEntryDto> list(String userId, int page, int size) {
        return PageResponse.of(history.findByUser(userId, PageRequest.of(page, size)), entries -> {
            List<SongDto> dtos = songService.toDtos(userId, entries.stream().map(PlayHistory::getSong).toList());
            return IntStream.range(0, entries.size())
                    .mapToObj(i -> new HistoryEntryDto(entries.get(i).getId(), dtos.get(i), entries.get(i).getPlayedAt()))
                    .toList();
        });
    }

    @Transactional
    public void clear(String userId) {
        history.deleteByUser(userId);
    }
}
