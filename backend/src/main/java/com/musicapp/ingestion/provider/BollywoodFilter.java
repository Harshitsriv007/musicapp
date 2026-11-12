package com.musicapp.ingestion.provider;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keeps search results on topic: a track is kept only when its title, artist, genre, tags or album
 * contain a whole word of the search term (e.g. "arijit" for "arijit singh") or a Bollywood keyword.
 */
final class BollywoodFilter {

    /** Always accepted, whatever was searched. */
    static final Set<String> BOLLYWOOD_WORDS = Set.of("bollywood", "hindi", "filmi");

    /** Too generic to show a track matches the search. */
    private static final Set<String> GENERIC_WORDS = Set.of(
            "song", "songs", "hits", "hit", "best", "new", "latest", "top", "old", "romantic", "love", "sad",
            "party", "music", "singer", "mashup", "remix", "the", "and", "90s", "80s", "70s",
            // common surnames: "singh" would match any Pawan Singh or Mika Singh upload
            "singh", "kumar", "khan", "sharma", "shah");

    private BollywoodFilter() {}

    private static final String NON_WORD = "[^\\p{L}\\p{N}]+";

    static Set<String> keywordsFor(String query) {
        Stream<String> fromQuery = words(query).filter(w -> w.length() >= 3 && !GENERIC_WORDS.contains(w));
        return Stream.concat(fromQuery, BOLLYWOOD_WORDS.stream()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Whole words only: "atif" must not match the artist "Beatifuldead". Underscores split words too. */
    static boolean mentionsAny(Set<String> keywords, String... fields) {
        return Arrays.stream(fields).filter(Objects::nonNull).flatMap(BollywoodFilter::words).anyMatch(keywords::contains);
    }

    private static Stream<String> words(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).replace('_', ' ').split(NON_WORD)).filter(w -> !w.isEmpty());
    }
}
