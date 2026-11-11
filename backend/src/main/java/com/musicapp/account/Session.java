package com.musicapp.account;

/** A successful sign-up or login: the bearer token to send as {@code Authorization: Bearer <token>}. */
public record Session(String token, long accountId, String username) {}
