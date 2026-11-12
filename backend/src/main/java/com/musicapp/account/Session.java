package com.musicapp.account;

import java.time.Instant;

/**
 * A successful sign-up or login: the bearer token to send as {@code Authorization: Bearer <token>}.
 * It stops working at {@code expiresAt}; the listener then has to log in again.
 */
public record Session(String token, long accountId, String username, Instant expiresAt) {}
