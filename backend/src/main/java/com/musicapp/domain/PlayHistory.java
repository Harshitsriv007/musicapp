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
import java.time.Instant;

@Entity
@Table(name = "play_history")
public class PlayHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id")
    private Song song;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    protected PlayHistory() {}

    public PlayHistory(String userId, Song song, Instant playedAt) {
        this.userId = userId;
        this.song = song;
        this.playedAt = playedAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Song getSong() { return song; }
    public Instant getPlayedAt() { return playedAt; }
}
