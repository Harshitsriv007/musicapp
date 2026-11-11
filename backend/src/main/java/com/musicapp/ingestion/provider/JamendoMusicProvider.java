package com.musicapp.ingestion.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.musicapp.config.AppProperties;
import com.musicapp.ingestion.SongEvent;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Jamendo (https://developer.jamendo.com) – Creative Commons licensed music. Needs a free
 * client id; enable with MUSICAPP_PROVIDERS_JAMENDO_ENABLED=true and JAMENDO_CLIENT_ID.
 */
@Component
@ConditionalOnProperty(prefix = "musicapp.providers.jamendo", name = "enabled", havingValue = "true")
public class JamendoMusicProvider implements MusicProvider {

    public static final String SOURCE = "jamendo";

    private final RestClient client;
    private final String clientId;
    private final String tags;
    private final int limit;

    public JamendoMusicProvider(RestClient.Builder builder, AppProperties props) {
        var jamendo = props.providers().jamendo();
        this.clientId = jamendo.clientId();
        this.tags = jamendo.tags();
        this.limit = Math.min(200, props.ingestion().limitPerQuery() * 2);
        this.client = builder.baseUrl(ProviderSupport.stripTrailingSlash(jamendo.baseUrl())).build();
    }

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public List<SongEvent> fetchLatest() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Jamendo client id is not configured");
        }
        JamendoResponse response = client.get()
                .uri(uri -> uri.path("/v3.0/tracks/")
                        .queryParam("client_id", clientId)
                        .queryParam("format", "json")
                        .queryParam("limit", limit)
                        .queryParam("tags", tags)
                        .queryParam("order", "releasedate_desc")
                        .queryParam("audioformat", "mp32")
                        .build())
                .retrieve()
                .body(JamendoResponse.class);
        if (response == null || response.results() == null) {
            return List.of();
        }
        if (response.headers() != null && "failed".equalsIgnoreCase(response.headers().status())) {
            throw new IllegalStateException("Jamendo error: " + response.headers().errorMessage());
        }
        return response.results().stream()
                .filter(t -> t.id() != null && t.name() != null && t.audio() != null && !t.audio().isBlank())
                .map(t -> new SongEvent(
                        SOURCE,
                        t.id(),
                        ProviderSupport.truncate(t.name(), 512),
                        ProviderSupport.truncate(t.artistName(), 255),
                        ProviderSupport.truncate(t.albumName(), 255),
                        null,
                        t.image(),
                        t.audio(),
                        t.duration(),
                        ProviderSupport.parseDate(t.releaseDate())))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JamendoResponse(Headers headers, List<JamendoTrack> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Headers(String status, @JsonProperty("error_message") String errorMessage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JamendoTrack(
            String id,
            String name,
            Integer duration,
            @JsonProperty("artist_name") String artistName,
            @JsonProperty("album_name") String albumName,
            @JsonProperty("releasedate") String releaseDate,
            String image,
            String audio) {}
}
