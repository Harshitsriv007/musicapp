package com.musicapp.enrichment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.musicapp.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * Finds the original release date of a song in MusicBrainz (https://musicbrainz.org, free, no key).
 * Only confident matches are used: same title and an artist that matches the uploader or the title.
 * Requests are spaced out to respect MusicBrainz's limit of one request per second.
 */
@Component
public class MusicBrainzClient {

    private static final int MIN_SCORE = 85;
    /** MusicBrainz answers 503 when busy or throttling: wait 2s, 4s, 8s before giving up. */
    private static final int MAX_ATTEMPTS = 4;

    private final RestClient client;
    private final Duration minInterval;
    private final Duration backoffBase;
    private final Clock clock;
    private long lastRequestNanos;

    public MusicBrainzClient(RestClient.Builder builder, AppProperties props, Clock clock) {
        var cfg = props.enrichment();
        this.client = builder
                .baseUrl(cfg.baseUrl().replaceAll("/$", ""))
                .defaultHeader("User-Agent", cfg.userAgent())
                .defaultHeader("Accept", "application/json")
                .build();
        this.minInterval = cfg.minInterval();
        this.backoffBase = minInterval.isZero() ? Duration.ZERO : Duration.ofSeconds(2);
        this.clock = clock;
    }

    public Optional<LocalDate> findOriginalReleaseDate(String title, String artist) {
        for (TitleCleaner.Candidate candidate : TitleCleaner.candidates(title, artist)) {
            Optional<LocalDate> date = lookup(candidate);
            if (date.isPresent()) {
                return date;
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> lookup(TitleCleaner.Candidate candidate) {
        String wanted = TitleCleaner.normalize(candidate.title());
        List<String> hints = candidate.artistHints().stream().map(TitleCleaner::normalize).toList();
        Response response = search("recording:\"" + escape(candidate.title()) + "\"");
        if (response == null || response.recordings() == null) {
            return Optional.empty();
        }
        LocalDate today = LocalDate.now(clock);
        return response.recordings().stream()
                .filter(r -> r.score() >= MIN_SCORE)
                .filter(r -> TitleCleaner.normalize(r.title()).equals(wanted))
                .filter(r -> r.artistNames().stream().map(TitleCleaner::normalize).anyMatch(a -> matchesAny(a, hints)))
                .map(r -> parseDate(r.firstReleaseDate()))
                .filter(Objects::nonNull)
                .filter(d -> !d.isAfter(today))
                .min(Comparator.naturalOrder());
    }

    private synchronized Response search(String query) {
        for (int attempt = 1; ; attempt++) {
            pause(minInterval.toMillis() - (System.nanoTime() - lastRequestNanos) / 1_000_000);
            try {
                return request(query);
            } catch (HttpServerErrorException.ServiceUnavailable | HttpClientErrorException.TooManyRequests busy) {
                if (attempt >= MAX_ATTEMPTS || Thread.currentThread().isInterrupted()) {
                    throw busy;
                }
                long retryAfterMs = Optional.ofNullable(busy.getResponseHeaders())
                        .map(h -> h.getFirst("Retry-After"))
                        .filter(v -> v.matches("\\d+"))
                        .map(v -> Long.parseLong(v) * 1000)
                        .orElse(0L);
                pause(Math.max(retryAfterMs, backoffBase.toMillis() << (attempt - 1)));
            }
        }
    }

    private void pause(long millis) {
        if (millis <= 0 || lastRequestNanos == 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Response request(String query) {
        try {
            return client.get()
                    .uri(uri -> uri.path("/ws/2/recording")
                            .queryParam("query", "{q}")
                            .queryParam("fmt", "json")
                            .queryParam("limit", 25)
                            .build(query))
                    .retrieve()
                    .body(Response.class);
        } finally {
            lastRequestNanos = System.nanoTime();
        }
    }

    /** Artist "arijit singh" matches hint "arijit singh", "arijit singh official" or "tum hi ho arijit singh". */
    private static boolean matchesAny(String artist, List<String> hints) {
        if (artist.length() < 2) {
            return false;
        }
        return hints.stream().anyMatch(h -> (" " + h + " ").contains(" " + artist + " ")
                || (h.length() >= 4 && (" " + artist + " ").contains(" " + h + " ")));
    }

    static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return switch (raw.length()) {
                case 4 -> LocalDate.of(Integer.parseInt(raw), 1, 1);
                case 7 -> LocalDate.parse(raw + "-01");
                default -> LocalDate.parse(raw.substring(0, 10));
            };
        } catch (DateTimeParseException | NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(List<Recording> recordings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Recording(String title, int score,
                     @JsonProperty("first-release-date") String firstReleaseDate,
                     @JsonProperty("artist-credit") List<ArtistCredit> artistCredit) {
        List<String> artistNames() {
            return artistCredit == null ? List.of()
                    : artistCredit.stream().map(ArtistCredit::name).filter(Objects::nonNull).toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ArtistCredit(String name) {}
}
