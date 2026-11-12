package com.musicapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A logged-in device. Identified by the SHA-256 of its bearer token. */
@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    protected AuthSession() {}

    public AuthSession(String tokenHash, UserAccount user, Instant now) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    public String getTokenHash() { return tokenHash; }
    public UserAccount getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void touch(Instant now) { this.lastUsedAt = now; }
}
