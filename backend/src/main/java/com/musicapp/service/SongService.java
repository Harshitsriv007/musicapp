package com.musicapp.service;

import com.musicapp.domain.Song;
import com.musicapp.ingestion.SongEvent;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.web.dto.PageResponse;
import com.musicapp.web.dto.SongDto;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SongService {

    /** "New songs" = latest releases first, then most recently ingested. */
    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("releaseDate"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final SongRepository songs;
    private final LikedSongRepository likes;

    public SongService(SongRepository songs, LikedSongRepository likes) {
        this.songs = songs;
        this.likes = likes;
    }

    /**
     * Inserts or refreshes a song coming from Kafka.
     *
     * @return the created song, or empty if it already existed (and was just refreshed)
     */
    @Transactional
    public Optional<SongDto> upsert(SongEvent e) {
        Optional<Song> existing = songs.findBySourceAndExternalId(e.source(), e.externalId());
        Song song = existing.orElseGet(() -> new Song(e.source(), e.externalId()));
        song.setTitle(e.title());
        song.setArtist(e.artist());
        song.setAlbum(e.album());
        song.setGenre(e.genre());
        song.setImageUrl(e.imageUrl());
        song.setStreamUrl(e.streamUrl());
        song.setDurationSeconds(e.durationSeconds());
        song.setReleaseDate(e.releaseDate());
        try {
            Song saved = songs.saveAndFlush(song);
            return existing.isPresent() ? Optional.empty() : Optional.of(SongDto.from(saved, false));
        } catch (DataIntegrityViolationException duplicate) {
            // Lost a race with a concurrent insert of the same song – treat as existing.
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<SongDto> list(String userId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, NEWEST_FIRST);
        Page<Song> result = (query == null || query.isBlank())
                ? songs.findAll(pageable)
                : songs.search(query.strip(), pageable);
        return PageResponse.of(result, list -> toDtos(userId, list));
    }

    @Transactional(readOnly = true)
    public List<SongDto> newest(String userId, int limit) {
        return toDtos(userId, songs.findAll(PageRequest.of(0, limit, NEWEST_FIRST)).getContent());
    }

    @Transactional(readOnly = true)
    public SongDto get(String userId, Long id) {
        Song song = songs.findById(id).orElseThrow(() -> new SongNotFoundException(id));
        return SongDto.from(song, likes.existsByUserIdAndSongId(userId, id));
    }

    List<SongDto> toDtos(String userId, List<Song> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> liked = likes.findLikedSongIds(userId, list.stream().map(Song::getId).toList());
        return list.stream().map(s -> SongDto.from(s, liked.contains(s.getId()))).toList();
    }
}
