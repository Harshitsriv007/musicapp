package com.musicapp.web.dto;

import com.musicapp.account.Session;

public record AuthResponse(String token, String username) {
    public static AuthResponse from(Session session) {
        return new AuthResponse(session.token(), session.username());
    }
}
