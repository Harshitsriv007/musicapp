package com.musicapp.ingestion.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.musicapp.config.AppProperties;
import com.musicapp.ingestion.SongEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Jamendo (https://developer.jamendo.com) – Creative Commons licensed music. Needs a free
 * client id; enable with JAMENDO_ENABLED=true and JAMENDO_CLIENT_ID.
 *
 * <p>Two kinds of run:
 * <ul>
 *   <li><b>Full sync</b> (first run, then every {@code fullSyncInterval}): pages through
 *       <em>every</em> track matching the tags and each search term, most popular first, up to
 *       {@code maxTracks} per query.</li>
 *   <li><b>Quick check</b> (all other runs): one request for the newest matching tracks.</li>
 * </ul>
 * Jamendo sometimes answers with an empty page (no {@code results_fullcount}) instead of the real
 * results, so requests are spaced by {@code requestInterval} and such pages are retried.
 *
 * <p>API terms: credit the artist and link back to the track on Jamendo (shareUrl), show its
 * licence, and only store audio when {@code audiodownload_allowed} is true. Jamendo is indie
 * music only – no Bollywood labels – and free for non-commercial use only.
 */
@Component
@ConditionalOnProperty(prefix = "musicapp.providers.jamendo", name = "enabled", havingValue = "true")
public class JamendoMusicProvider implements MusicProvider {

    public static final String SOURCE = "jamendo";
    /** Jamendo's maximum page size. */
    static final int PAGE_SIZE = 200;
    /** Attempts per page when Jamendo answers with a spurious empty page. */
    static final int MAX_ATTEMPTS = 4;

    private static final Logger log = LoggerFactory.getLogger(JamendoMusicProvider.class);

    private final RestClient client;
    private final AppProperties.Jamendo config;
    private final Clock clock;
    private Instant lastFullSync;

    public JamendoMusicProvider(RestClient.Builder builder, AppProperties props, Clock clock) {
        this.config = props.providers().jamendo();
        this.clock = clock;
        this.client = builder.baseUrl(ProviderSupport.stripTrailingSlash(config.baseUrl())).build();
    }

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public synchronized List<SongEvent> fetchLatest() {
        if (config.clientId() == null || config.clientId().isBlank()) {
            throw new IllegalStateException("Jamendo client id is not configured");
        }
        Instant now = Instant.now(clock);
        boolean full = lastFullSync == null || !now.isBefore(lastFullSync.plus(config.fullSyncInterval()));

        Map<String, SongEvent> tracks = new LinkedHashMap<>();
        List<Query> queries = queries(full);
        for (Query query : queries) {
            fetchAll(query, full ? config.maxTracks() : PAGE_SIZE, tracks);
        }
        if (full) {
            lastFullSync = now;
        }
        log.info("Jamendo {} sync: {} tracks from {} queries", full ? "full" : "quick", tracks.size(), queries.size());
        return List.copyOf(tracks.values());
    }

    /** Full sync: tags + every search term, most popular first. Quick check: the newest tagged tracks. */
    private List<Query> queries(boolean full) {
        List<Query> queries = new ArrayList<>();
        if (!config.tags().isBlank()) {
            queries.add(new Query("fuzzytags", config.tags(), full ? "popularity_total" : "releasedate_desc"));
        }
        if (full) {
            config.searches().forEach(term -> queries.add(new Query("search", term, "popularity_total")));
        }
        return queries;
    }

    private void fetchAll(Query query, int max, Map<String, SongEvent> into) {
        for (int offset = 0; offset < max; offset += PAGE_SIZE) {
            if (offset > 0 || !into.isEmpty()) {
                pause();
            }
            JamendoResponse page = fetchPageWithRetry(query, offset, Math.min(PAGE_SIZE, max - offset));
            List<JamendoTrack> results = page.results();
            results.stream()
                    .filter(t -> t.id() != null && t.name() != null && t.audio() != null && !t.audio().isBlank())
                    .forEach(t -> into.putIfAbsent(t.id(), toEvent(t)));
            Integer total = page.headers() == null ? null : page.headers().fullCount();
            if (results.size() < PAGE_SIZE || (total != null && offset + results.size() >= total)) {
                return;
            }
        }
    }

    /**
     * A real "no results" answer carries {@code results_fullcount: 0}; an empty page without it is
     * Jamendo failing quietly, so it is retried with a growing pause.
     */
    private JamendoResponse fetchPageWithRetry(Query query, int offset, int limit) {
        for (int attempt = 1; ; attempt++) {
            JamendoResponse page = fetchPage(query, offset, limit);
            boolean spurious = page.results().isEmpty()
                    && (page.headers() == null || page.headers().fullCount() == null);
            if (!spurious) {
                return page;
            }
            if (attempt == MAX_ATTEMPTS) {
                log.warn("Jamendo kept returning empty pages for {}={} at offset {}; skipping",
                        query.param(), query.value(), offset);
                return page;
            }
            for (int i = 0; i < attempt; i++) {
                pause();
            }
        }
    }

    private JamendoResponse fetchPage(Query query, int offset, int limit) {
        JamendoResponse response = client.get()
                .uri(uri -> uri.path("/v3.0/tracks/")
                        .queryParam("client_id", config.clientId())
                        .queryParam("format", "json")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("fullcount", true)
                        // As a URI variable so each "+" is sent as %2B: Jamendo reads a raw "+" as a space
                        // and matches nothing.
                        .queryParam(query.param(), "{value}")
                        .queryParam("order", query.order())
                        .queryParam("audioformat", "mp32")
                        .build(query.value()))
                .retrieve()
                .body(JamendoResponse.class);
        if (response != null && response.headers() != null && "failed".equalsIgnoreCase(response.headers().status())) {
            throw new IllegalStateException("Jamendo error: " + response.headers().errorMessage());
        }
        if (response == null || response.results() == null) {
            return new JamendoResponse(response == null ? null : response.headers(), List.of());
        }
        return response;
    }

    private void pause() {
        Duration wait = config.requestInterval();
        if (wait.isZero() || wait.isNegative()) {
            return;
        }
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Jamendo sync interrupted", e);
        }
    }

    private static SongEvent toEvent(JamendoTrack t) {
        return new SongEvent(
                SOURCE,
                t.id(),
                ProviderSupport.truncate(t.name(), 512),
                ProviderSupport.truncate(t.artistName(), 255),
                ProviderSupport.truncate(t.albumName(), 255),
                null,
                t.image(),
                t.audio(),
                t.duration(),
                ProviderSupport.parseDate(t.releaseDate()),
                t.shareUrl(),
                t.licenseUrl(),
                Boolean.TRUE.equals(t.downloadAllowed()));
    }

    private record Query(String param, String value, String order) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JamendoResponse(Headers headers, List<JamendoTrack> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Headers(String status, @JsonProperty("error_message") String errorMessage,
                   @JsonProperty("results_fullcount") Integer fullCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JamendoTrack(
            String id,
            String name,
            Integer duration,
            @JsonProperty("artist_name") String artistName,
            @JsonProperty("album_name") String albumName,
            @JsonProperty("releasedate") String releaseDate,
            String image,
            String audio,
            @JsonProperty("shareurl") String shareUrl,
            @JsonProperty("license_ccurl") String licenseUrl,
            @JsonProperty("audiodownload_allowed") Boolean downloadAllowed) {}
}
