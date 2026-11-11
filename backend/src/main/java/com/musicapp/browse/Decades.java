package com.musicapp.browse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Decade folders: "latest" (released in the last 12 months) plus 50s, 60s … up to the current
 * decade. The 50s folder also holds anything older than 1950.
 */
public final class Decades {

    public static final String LATEST = "latest";
    public static final int FIRST = 1950;

    /** Inclusive date range; a null bound means open-ended. */
    public record DateRange(LocalDate from, LocalDate to) {}

    private Decades() {}

    public static int decadeOf(int year) {
        return Math.max(FIRST, year / 10 * 10);
    }

    /** Decade start years, newest first: e.g. 2020, 2010, … 1950. */
    public static List<Integer> all(LocalDate today) {
        List<Integer> decades = new ArrayList<>();
        for (int d = decadeOf(today.getYear()); d >= FIRST; d -= 10) {
            decades.add(d);
        }
        return decades;
    }

    /** 1990 → "90s", 2000 → "00s", 2020 → "20s". */
    public static String label(int decade) {
        return String.format("%02ds", decade % 100);
    }

    public static DateRange range(String key, LocalDate today) {
        if (LATEST.equalsIgnoreCase(key)) {
            return new DateRange(today.minusYears(1), null);
        }
        int decade;
        try {
            decade = Integer.parseInt(key);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown decade '" + key + "' (use latest, 1950, 1960 … )");
        }
        if (decade % 10 != 0 || decade < FIRST || decade > decadeOf(today.getYear())) {
            throw new IllegalArgumentException("Unknown decade '" + key + "'");
        }
        LocalDate from = decade == FIRST ? null : LocalDate.of(decade, 1, 1);
        return new DateRange(from, LocalDate.of(decade + 9, 12, 31));
    }
}
