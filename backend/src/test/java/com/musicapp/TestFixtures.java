package com.musicapp;

import com.musicapp.config.AppProperties;
import com.musicapp.domain.Song;
import com.musicapp.ingestion.SongEvent;
import java.time.LocalDate;
import java.util.List;

public final class TestFixtures {

    private TestFixtures() {}

    public static AppProperties props(String audiusUrl, List<String> queries) {
        return new AppProperties(
                new AppProperties.Ingestion("-", false, queries, 10),
                new AppProperties.Kafka("music.songs.discovered", "music.songs.download-requested", 1),
                new AppProperties.Providers(
                        new AppProperties.Audius(true, audiusUrl, "testapp"),
                        AppProperties.Jamendo.of(true, "https://jamendo.test", "cid", "bollywood")),
                new AppProperties.Cors(List.of("*")),
                new AppProperties.Download(10_000),
                new AppProperties.Enrichment(true, "https://mb.test", "DhunTest/1.0", 30, java.time.Duration.ZERO));
    }

    public static SongEvent event(String id, String title) {
        return new SongEvent("audius", id, title, "Arijit Singh", null, "Bollywood",
                "https://img/" + id + ".jpg", "https://stream/" + id, 240, LocalDate.of(2024, 1, 1));
    }

    public static Song song(long id, String title) {
        Song s = new Song("audius", "ext-" + id).withId(id);
        s.setTitle(title);
        s.setArtist("Arijit Singh");
        s.setStreamUrl("https://stream/" + id);
        return s;
    }
}
