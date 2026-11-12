package com.musicapp.web.dto;

import com.musicapp.account.Session;
import java.time.Instant;

/** {@code expiresAt}: when the token stops working and the client must show the login screen. */
public record AuthResponse(String token, String username, Instant expiresAt) {
    public static AuthResponse from(Session session) {
        return new AuthResponse(session.token(), session.username(), session.expiresAt());
    }
}
