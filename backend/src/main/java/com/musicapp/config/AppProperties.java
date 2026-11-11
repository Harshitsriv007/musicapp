package com.musicapp.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly typed view of the {@code musicapp.*} configuration tree. */
@ConfigurationProperties(prefix = "musicapp")
public record AppProperties(Ingestion ingestion, Kafka kafka, Providers providers, Cors cors, Download download) {

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

    public record Kafka(String songsTopic, String downloadsTopic, int partitions) {}

    /**
     * @param maxBytes   largest audio file we are willing to store per song
     * @param autoOnPlay store every song the first time someone plays it, so later plays come from our DB
     */
    public record Download(long maxBytes, boolean autoOnPlay) {
        public Download {
            maxBytes = maxBytes <= 0 ? 100L * 1024 * 1024 : maxBytes;
        }
    }

    public record Providers(Audius audius, Jamendo jamendo) {}

    public record Audius(boolean enabled, String baseUrl, String appName) {}

    public record Jamendo(boolean enabled, String baseUrl, String clientId, String tags) {}

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of("*") : List.copyOf(allowedOrigins);
        }
    }
}
