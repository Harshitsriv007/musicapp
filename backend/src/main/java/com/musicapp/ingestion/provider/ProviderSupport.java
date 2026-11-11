package com.musicapp.ingestion.provider;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

final class ProviderSupport {

    private ProviderSupport() {}

    /** Accepts "2024-05-01", "2024-05-01T10:00:00Z" and similar; returns null if unparseable. */
    static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return raw.length() >= 10 ? LocalDate.parse(raw.substring(0, 10)) : null;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
