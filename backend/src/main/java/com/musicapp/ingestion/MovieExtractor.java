package com.musicapp.ingestion;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Works out which movie (or album) a song belongs to. Free music APIs have no "movie" field,
 * so we read the conventions Bollywood uploads use in titles, e.g.
 * {@code Kesariya (From "Brahmastra")}, {@code Tum Hi Ho [From the movie Aashiqui 2]},
 * {@code Chaleya | Movie: Jawan}; otherwise we fall back to the album name.
 */
public final class MovieExtractor {

    private static final String QUOTES = "\"“”'‘’";
    private static final List<Pattern> TITLE_PATTERNS = List.of(
            Pattern.compile("[(\\[]\\s*from\\s+(?:the\\s+)?(?:movie|film)?\\s*[" + QUOTES + "]([^" + QUOTES + "]{2,80})[" + QUOTES + "]\\s*[)\\]]",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]\\s*from\\s+(?:the\\s+)?(?:movie|film)\\s+([^)\\]]{2,80})[)\\]]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:movie|film)\\s*[:\\-–]\\s*([^|()\\[\\]]{2,60})", Pattern.CASE_INSENSITIVE));

    private MovieExtractor() {}

    public static String extract(String title, String album) {
        if (title != null) {
            for (Pattern pattern : TITLE_PATTERNS) {
                Matcher m = pattern.matcher(title);
                if (m.find()) {
                    String movie = clean(m.group(1));
                    if (movie != null) {
                        return movie;
                    }
                }
            }
        }
        return clean(album);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip().replaceAll("\\s+", " ");
        if (v.length() < 2) {
            return null;
        }
        return v.length() > 255 ? v.substring(0, 255) : v;
    }
}
