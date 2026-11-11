package com.musicapp.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly typed view of the {@code musicapp.*} configuration tree. */
@ConfigurationProperties(prefix = "musicapp")
public record AppProperties(Ingestion ingestion, Kafka kafka, Providers providers, Cors cors) {

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

    public record Kafka(String songsTopic, int partitions) {
        public String deadLetterTopic() {
            return songsTopic + ".DLT";
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
