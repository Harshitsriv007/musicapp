package com.musicapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "liked_songs", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "song_id"}))
public class LikedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id")
    private Song song;

    @Column(name = "liked_at", nullable = false)
    private Instant likedAt;

    protected LikedSong() {}

    public LikedSong(String userId, Song song, Instant likedAt) {
        this.userId = userId;
        this.song = song;
        this.likedAt = likedAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Song getSong() { return song; }
    public Instant getLikedAt() { return likedAt; }
}
