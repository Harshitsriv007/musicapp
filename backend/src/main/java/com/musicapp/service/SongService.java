package com.musicapp.service;

import com.musicapp.browse.Decades;
import com.musicapp.domain.Song;
import com.musicapp.ingestion.MovieExtractor;
import com.musicapp.ingestion.SongEvent;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserDownloadRepository;
import com.musicapp.web.dto.PageResponse;
import com.musicapp.web.dto.SongDto;
import java.time.Clock;
import java.time.LocalDate;
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
    private final UserDownloadRepository downloads;
    private final Clock clock;

    public SongService(SongRepository songs, LikedSongRepository likes, UserDownloadRepository downloads, Clock clock) {
        this.songs = songs;
        this.likes = likes;
        this.downloads = downloads;
        this.clock = clock;
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
        song.setMovie(MovieExtractor.extract(e.title(), e.album()));
        song.setGenre(e.genre());
        song.setImageUrl(e.imageUrl());
        song.setStreamUrl(e.streamUrl());
        song.setDurationSeconds(e.durationSeconds());
        song.setReleaseDate(e.releaseDate());
        try {
            Song saved = songs.saveAndFlush(song);
            return existing.isPresent() ? Optional.empty() : Optional.of(SongDto.from(saved, false, false));
        } catch (DataIntegrityViolationException duplicate) {
            // Lost a race with a concurrent insert of the same song – treat as existing.
            return Optional.empty();
        }
    }

    /** {@code userId} is null for a visitor who is not logged in: no likes or downloads are shown. */
    @Transactional(readOnly = true)
    public PageResponse<SongDto> list(String userId, SongFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, NEWEST_FIRST);
        if (filter.downloadedOnly() && userId == null) {
            return PageResponse.of(Page.<Song>empty(pageable), list -> List.of());
        }
        Decades.DateRange released = filter.decade() == null
                ? new Decades.DateRange(null, null)
                : Decades.range(filter.decade(), LocalDate.now(clock));
        Page<Song> result = songs.findFiltered(filter.query(), filter.downloadedOnly() ? userId : null,
                released.from(), released.to(), filter.artist(), filter.movie(), pageable);
        return PageResponse.of(result, list -> toDtos(userId, list));
    }

    @Transactional(readOnly = true)
    public List<SongDto> newest(String userId, int limit) {
        return toDtos(userId, songs.findAll(PageRequest.of(0, limit, NEWEST_FIRST)).getContent());
    }

    @Transactional(readOnly = true)
    public SongDto get(String userId, Long id) {
        Song song = songs.findById(id).orElseThrow(() -> new SongNotFoundException(id));
        return toDtos(userId, List.of(song)).getFirst();
    }

    /** Songs as this listener sees them: their own likes and downloads only. */
    public List<SongDto> toDtos(String userId, List<Song> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        if (userId == null) {
            return list.stream().map(s -> SongDto.from(s, false, false)).toList();
        }
        List<Long> ids = list.stream().map(Song::getId).toList();
        Set<Long> liked = likes.findLikedSongIds(userId, ids);
        Set<Long> downloaded = downloads.findDownloadedSongIds(userId, ids);
        return list.stream().map(s -> SongDto.from(s, liked.contains(s.getId()), downloaded.contains(s.getId()))).toList();
    }
}
