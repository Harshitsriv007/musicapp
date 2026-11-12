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

/** A song in one listener's Downloads. The audio is shared; this row is what makes it "theirs". */
@Entity
@Table(name = "user_downloads", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "song_id"}))
public class UserDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id")
    private Song song;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    protected UserDownload() {}

    public UserDownload(String userId, Song song, Instant requestedAt) {
        this.userId = userId;
        this.song = song;
        this.requestedAt = requestedAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Song getSong() { return song; }
    public Instant getRequestedAt() { return requestedAt; }
}
