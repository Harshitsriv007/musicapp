package com.musicapp.ingestion.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.musicapp.config.AppProperties;
import com.musicapp.ingestion.SongEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Audius (https://audius.co) – free, open music API, no key required. Artists upload their own
 * tracks, so the catalogue includes plenty of Bollywood / Hindi music that is free to stream.
 */
@Component
@ConditionalOnProperty(prefix = "musicapp.providers.audius", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AudiusMusicProvider implements MusicProvider {

    public static final String SOURCE = "audius";

    private final RestClient client;
    private final String baseUrl;
    private final String appName;
    private final List<String> queries;
    private final int limit;

    public AudiusMusicProvider(RestClient.Builder builder, AppProperties props) {
        var audius = props.providers().audius();
        this.baseUrl = ProviderSupport.stripTrailingSlash(audius.baseUrl());
        this.appName = audius.appName();
        this.queries = props.ingestion().queries();
        this.limit = props.ingestion().limitPerQuery();
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public List<SongEvent> fetchLatest() {
        List<SongEvent> songs = new ArrayList<>();
        for (String query : queries) {
            AudiusResponse response = client.get()
                    .uri(uri -> uri.path("/v1/tracks/search")
                            .queryParam("query", query)
                            .queryParam("limit", limit)
                            .queryParam("app_name", appName)
                            .build())
                    .retrieve()
                    .body(AudiusResponse.class);
            if (response == null || response.data() == null) {
                continue;
            }
            response.data().stream()
                    .filter(AudiusTrack::isPlayable)
                    .map(this::toEvent)
                    .forEach(songs::add);
        }
        return songs;
    }

    private SongEvent toEvent(AudiusTrack t) {
        var releaseDate = ProviderSupport.parseDate(t.releaseDate());
        if (releaseDate == null) {
            releaseDate = ProviderSupport.parseDate(t.createdAt());
        }
        return new SongEvent(
                SOURCE,
                t.id(),
                ProviderSupport.truncate(t.title(), 512),
                ProviderSupport.truncate(t.user() == null ? null : t.user().name(), 255),
                null,
                ProviderSupport.truncate(t.genre(), 255),
                t.artworkUrl(),
                baseUrl + "/v1/tracks/" + t.id() + "/stream?app_name=" + appName,
                t.duration(),
                releaseDate);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AudiusResponse(List<AudiusTrack> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AudiusUser(String name, String handle) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AudiusTrack(
            String id,
            String title,
            String genre,
            Integer duration,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("created_at") String createdAt,
            Map<String, Object> artwork,
            AudiusUser user,
            @JsonProperty("is_delete") Boolean deleted,
            @JsonProperty("is_available") Boolean available,
            @JsonProperty("is_stream_gated") Boolean streamGated) {

        boolean isPlayable() {
            return id != null && title != null
                    && !Boolean.TRUE.equals(deleted)
                    && !Boolean.FALSE.equals(available)
                    && !Boolean.TRUE.equals(streamGated);
        }

        String artworkUrl() {
            if (artwork == null) {
                return null;
            }
            return List.of("480x480", "1000x1000", "150x150").stream()
                    .map(artwork::get)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .findFirst()
                    .orElse(null);
        }
    }
}
