package com.musicapp.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly typed view of the {@code musicapp.*} configuration tree. */
@ConfigurationProperties(prefix = "musicapp")
public record AppProperties(Ingestion ingestion, Kafka kafka, Providers providers, Cors cors, Download download,
                            Enrichment enrichment) {

    /**
     * @param cron          Spring cron for the scheduled fetch ("-" disables it)
     * @param runOnStartup  fetch once as soon as the app is ready
     * @param queries       search terms sent to the music providers
     * @param limitPerQuery max songs requested per query
     */
    public record Ingestion(String cron, boolean runOnStartup, List<String> queries, int limitPerQuery) {
        public Ingestion {
            queries = queries == null ? List.of("bollywood") : List.copyOf(queries);
            limitPerQuery = limitPerQuery <= 0 ? 25 : limitPerQuery;
        }
    }

    /**
     * Original-release-year lookup in MusicBrainz (drives the decade folders).
     *
     * @param userAgent   MusicBrainz asks every client to identify itself (add contact info if you can)
     * @param minInterval MusicBrainz allows about one request per second
     */
    public record Enrichment(boolean enabled, String baseUrl, String userAgent, int batchSize,
                             java.time.Duration minInterval) {
        public Enrichment {
            batchSize = batchSize <= 0 ? 30 : batchSize;
            minInterval = minInterval == null ? java.time.Duration.ofMillis(1100) : minInterval;
        }
    }

    public record Kafka(String songsTopic, String downloadsTopic, int partitions) {}

    /**
     * Songs stream live from the music API; only songs the user downloads are stored in MySQL.
     *
     * @param maxBytes largest audio file we are willing to store per song
     */
    public record Download(long maxBytes) {
        public Download {
            maxBytes = maxBytes <= 0 ? 100L * 1024 * 1024 : maxBytes;
        }
    }

    public record Providers(Audius audius, Jamendo jamendo) {}

    public record Audius(boolean enabled, String baseUrl, String appName) {}

    /**
     * @param tags             Jamendo tags, "+"-separated; a track matching any of them is fetched
     * @param searches         free-text searches also run on a full sync (track, album, artist names)
     * @param maxTracks        most tracks fetched per tag list or search term on a full sync
     * @param fullSyncInterval time between full syncs; other runs only fetch the newest tracks
     * @param requestInterval  pause between requests (Jamendo returns empty pages when called too fast)
     */
    public record Jamendo(boolean enabled, String baseUrl, String clientId, String tags, List<String> searches,
                          int maxTracks, java.time.Duration fullSyncInterval, java.time.Duration requestInterval) {
        public Jamendo {
            tags = tags == null ? "" : tags.strip();
            searches = searches == null ? List.of()
                    : searches.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
            maxTracks = maxTracks <= 0 ? 2000 : maxTracks;
            fullSyncInterval = fullSyncInterval == null ? java.time.Duration.ofHours(6) : fullSyncInterval;
            requestInterval = requestInterval == null ? java.time.Duration.ofSeconds(1) : requestInterval;
        }

        /** For tests and callers that only care about the tags. */
        public static Jamendo of(boolean enabled, String baseUrl, String clientId, String tags) {
            return new Jamendo(enabled, baseUrl, clientId, tags, List.of(), 0, null, java.time.Duration.ZERO);
        }
    }

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of("*") : List.copyOf(allowedOrigins);
        }
    }
}
