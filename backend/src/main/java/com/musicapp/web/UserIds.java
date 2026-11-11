package com.musicapp.web;

import java.util.regex.Pattern;

/**
 * The app has no login: each device generates a random id and sends it in {@code X-User-Id},
 * so likes and history are kept per device/user.
 */
public final class UserIds {

    public static final String HEADER = "X-User-Id";
    public static final String DEFAULT = "guest";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private UserIds() {}

    public static String require(String raw) {
        if (raw == null || !VALID.matcher(raw).matches()) {
            throw new IllegalArgumentException(HEADER + " must match " + VALID.pattern());
        }
        return raw;
    }
}
