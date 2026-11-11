package com.musicapp.enrichment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns messy upload titles ("ATIF ASLAM - RONA CHADITA (Official Video)_128k") into clean
 * song titles to look up, plus artist hints taken from the other parts of the title.
 */
public final class TitleCleaner {

    /** A lookup attempt: the song title to search for and names that may be its artist. */
    public record Candidate(String title, List<String> artistHints) {}

    /** Not an original recording – keep the upload date, never look these up. */
    private static final Pattern NOT_ORIGINAL = Pattern.compile(
            "(?i)\\b(mash ?up|remix|re-?mix|lo-?fi|slowed|reverb|cover|medley|non-?stop|jukebox|8d|nightcore|instrumental|karaoke|reprise)\\b");
    private static final Pattern BRACKETS = Pattern.compile("[(\\[{][^)\\]}]*[)\\]}]");
    private static final Pattern FEATURING = Pattern.compile("(?i)\\s+(ft\\.?|feat\\.?|featuring)\\s+.*$");
    private static final Pattern NOISE = Pattern.compile(
            "(?i)(_?\\d{2,3}k\\b|\\bofficial( music)? video\\b|\\blyric(al|s)?( video)?\\b|\\bfull (song|video)\\b|\\b(hd|4k|audio|video song)\\b|\\bsong\\b$)");

    private TitleCleaner() {}

    public static boolean isOriginalRecording(String title) {
        return title != null && !NOT_ORIGINAL.matcher(title).find();
    }

    /** Up to two candidates: "A - B" may be "Artist - Title" or "Title - Artist/Movie". */
    public static List<Candidate> candidates(String rawTitle, String uploader) {
        if (!isOriginalRecording(rawTitle)) {
            return List.of();
        }
        String t = BRACKETS.matcher(rawTitle).replaceAll(" ");
        t = t.split("\\s[|｜]\\s|\\|")[0];
        t = t.replace('_', ' ');
        t = NOISE.matcher(t).replaceAll(" ");
        List<String> parts = new ArrayList<>();
        for (String p : t.split("\\s+[-–—]\\s+")) {
            String cleaned = FEATURING.matcher(p).replaceAll("").strip().replaceAll("\\s+", " ");
            if (normalize(cleaned).length() >= 2) {
                parts.add(cleaned);
            }
        }
        if (parts.isEmpty()) {
            return List.of();
        }
        List<Candidate> result = new ArrayList<>();
        if (parts.size() == 1) {
            result.add(new Candidate(parts.getFirst(), hints(uploader, rawTitle)));
        } else {
            // "Title - Artist" is the more common upload format; then "Artist - Title".
            result.add(new Candidate(parts.get(0), hints(uploader, rawTitle, parts.get(1))));
            result.add(new Candidate(parts.get(1), hints(uploader, rawTitle, parts.get(0))));
        }
        return result;
    }

    /** Lower-case, accents removed, only letters/digits separated by single spaces. */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    private static List<String> hints(String... values) {
        Set<String> hints = new LinkedHashSet<>();
        for (String v : values) {
            if (v != null && !normalize(v).isEmpty()) {
                hints.add(v);
            }
        }
        return List.copyOf(hints);
    }
}
