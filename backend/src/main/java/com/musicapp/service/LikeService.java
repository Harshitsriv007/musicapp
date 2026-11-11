package com.musicapp.service;

import com.musicapp.domain.LikedSong;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.web.dto.LikedSongDto;
import com.musicapp.web.dto.PageResponse;
import com.musicapp.web.dto.SongDto;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikedSongRepository likes;
    private final SongRepository songs;
    private final Clock clock;

    public LikeService(LikedSongRepository likes, SongRepository songs, Clock clock) {
        this.likes = likes;
        this.songs = songs;
        this.clock = clock;
    }

    /** Idempotent: liking an already-liked song is a no-op. */
    @Transactional
    public void like(String userId, Long songId) {
        var song = songs.findById(songId).orElseThrow(() -> new SongNotFoundException(songId));
        if (likes.existsByUserIdAndSongId(userId, songId)) {
            return;
        }
        try {
            likes.saveAndFlush(new LikedSong(userId, song, Instant.now(clock)));
        } catch (DataIntegrityViolationException alreadyLiked) {
            // concurrent double-click: the other request won, result is the same
        }
    }

    @Transactional
    public void unlike(String userId, Long songId) {
        likes.deleteByUserAndSong(userId, songId);
    }

    /** Like or unlike many songs at once (a whole folder). Unknown ids are ignored. */
    @Transactional
    public void setLiked(String userId, Collection<Long> songIds, boolean liked) {
        Set<Long> ids = new LinkedHashSet<>(songIds);
        if (!liked) {
            likes.deleteByUserAndSongs(userId, ids);
            return;
        }
        Set<Long> already = likes.findLikedSongIds(userId, ids);
        Instant now = Instant.now(clock);
        List<LikedSong> toSave = songs.findAllById(ids).stream()
                .filter(s -> !already.contains(s.getId()))
                .map(s -> new LikedSong(userId, s, now))
                .toList();
        likes.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    public PageResponse<LikedSongDto> list(String userId, int page, int size) {
        return PageResponse.of(likes.findByUser(userId, PageRequest.of(page, size)),
                list -> list.stream().map(l -> new LikedSongDto(SongDto.from(l.getSong(), true), l.getLikedAt())).toList());
    }
}
