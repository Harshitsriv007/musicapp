package com.musicapp.web;

import java.util.regex.Pattern;

/**
 * Who a request belongs to. Logged-in listeners are identified by their account
 * ({@code acct_<id>}, so likes and history follow them to every device). Anonymous devices send
 * a random id in {@code X-User-Id}; that id can never use the reserved account prefix.
 */
public final class UserIds {

    public static final String HEADER = "X-User-Id";
    public static final String DEFAULT = "guest";
    static final String ACCOUNT_PREFIX = "acct_";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private UserIds() {}

    /** Validates an anonymous device id. */
    public static String require(String raw) {
        if (raw == null || !VALID.matcher(raw).matches()) {
            throw new IllegalArgumentException(HEADER + " must match " + VALID.pattern());
        }
        if (raw.startsWith(ACCOUNT_PREFIX)) {
            throw new IllegalArgumentException(HEADER + " must not start with " + ACCOUNT_PREFIX);
        }
        return raw;
    }

    public static String forAccount(long accountId) {
        return ACCOUNT_PREFIX + accountId;
    }
}
