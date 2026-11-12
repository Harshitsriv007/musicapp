package com.musicapp.ingestion.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.musicapp.TestFixtures;
import com.musicapp.config.AppProperties;
import com.musicapp.ingestion.SongEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JamendoMusicProviderTest {

    private static final Instant NOW = Instant.parse("2026-11-12T00:00:00Z");

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private JamendoMusicProvider provider(AppProperties.Jamendo jamendo, Clock clock) {
        var base = TestFixtures.props("https://audius.test", List.of("x"));
        var props = new AppProperties(base.ingestion(), base.kafka(),
                new AppProperties.Providers(base.providers().audius(), jamendo),
                base.cors(), base.download(), base.enrichment());
        return new JamendoMusicProvider(builder, props, clock);
    }

    private JamendoMusicProvider provider(String tags, List<String> searches) {
        return provider(new AppProperties.Jamendo(true, "https://jamendo.test", "cid", tags, searches, 0,
                Duration.ofHours(6), Duration.ZERO), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static String page(int from, int count) {
        String tracks = IntStream.range(from, from + count)
                .mapToObj(i -> "{\"id\":\"t" + i + "\",\"name\":\"Track " + i + "\",\"audio\":\"https://audio/" + i + "\"}")
                .collect(Collectors.joining(","));
        return "{\"headers\":{\"status\":\"success\",\"results_fullcount\":100000},\"results\":[" + tracks + "]}";
    }

    @Test
    void mapsResultsAndSkipsTracksWithoutAudio() {
        var provider = provider("bollywood", List.of());
        server.expect(queryParam("client_id", "cid"))
                .andExpect(queryParam("fuzzytags", "bollywood"))
                .andExpect(queryParam("order", "popularity_total"))
                .andRespond(withSuccess("""
                        {"headers":{"status":"success","results_fullcount":2},"results":[
                          {"id":"j1","name":"Dil Se","duration":300,"artist_name":"AR","album_name":"Album",
                           "releasedate":"2023-03-04","image":"https://img/j1.jpg","audio":"https://audio/j1.mp3",
                           "shareurl":"https://www.jamendo.com/track/j1",
                           "license_ccurl":"http://creativecommons.org/licenses/by-nc-sa/3.0/",
                           "audiodownload_allowed":false},
                          {"id":"j2","name":"No audio","audio":""}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<SongEvent> songs = provider.fetchLatest();

        assertThat(songs).singleElement().satisfies(s -> {
            assertThat(s.source()).isEqualTo("jamendo");
            assertThat(s.title()).isEqualTo("Dil Se");
            assertThat(s.album()).isEqualTo("Album");
            assertThat(s.streamUrl()).isEqualTo("https://audio/j1.mp3");
            assertThat(s.releaseDate()).isEqualTo(LocalDate.of(2023, 3, 4));
            assertThat(s.shareUrl()).isEqualTo("https://www.jamendo.com/track/j1");
            assertThat(s.licenseUrl()).isEqualTo("http://creativecommons.org/licenses/by-nc-sa/3.0/");
            assertThat(s.downloadAllowed()).isFalse();
        });
        server.verify();
    }

    @Test
    void fullSyncPagesThroughEveryMatchingTrackThenRunsEachSearch() {
        var provider = provider("bollywood", List.of("hindi"));
        server.expect(queryParam("offset", "0")).andExpect(queryParam("fuzzytags", "bollywood"))
                .andRespond(withSuccess(page(0, 200), MediaType.APPLICATION_JSON));
        server.expect(queryParam("offset", "200")).andExpect(queryParam("fuzzytags", "bollywood"))
                .andRespond(withSuccess(page(200, 50), MediaType.APPLICATION_JSON));
        // the search overlaps the tag results by 10 tracks: they are only returned once
        server.expect(queryParam("search", "hindi")).andExpect(queryParam("offset", "0"))
                .andRespond(withSuccess(page(240, 30), MediaType.APPLICATION_JSON));

        List<SongEvent> songs = provider.fetchLatest();

        assertThat(songs).hasSize(270);
        assertThat(songs).extracting(SongEvent::externalId).doesNotHaveDuplicates();
        server.verify();
    }

    @Test
    void fullSyncStopsAtMaxTracks() {
        var provider = provider(new AppProperties.Jamendo(true, "https://jamendo.test", "cid", "bollywood",
                List.of(), 300, Duration.ofHours(6), Duration.ZERO), Clock.fixed(NOW, ZoneOffset.UTC));
        server.expect(queryParam("offset", "0")).andExpect(queryParam("limit", "200"))
                .andRespond(withSuccess(page(0, 200), MediaType.APPLICATION_JSON));
        server.expect(queryParam("offset", "200")).andExpect(queryParam("limit", "100"))
                .andRespond(withSuccess(page(200, 100), MediaType.APPLICATION_JSON));

        assertThat(provider.fetchLatest()).hasSize(300);
        server.verify();
    }

    @Test
    void betweenFullSyncsOnlyTheNewestTracksAreFetched() {
        var provider = provider("bollywood", List.of("hindi"));
        server.expect(queryParam("order", "popularity_total"))
                .andRespond(withSuccess(page(0, 5), MediaType.APPLICATION_JSON));
        server.expect(queryParam("search", "hindi"))
                .andRespond(withSuccess(page(5, 5), MediaType.APPLICATION_JSON));
        server.expect(queryParam("order", "releasedate_desc")).andExpect(queryParam("fuzzytags", "bollywood"))
                .andRespond(withSuccess(page(10, 3), MediaType.APPLICATION_JSON));

        assertThat(provider.fetchLatest()).hasSize(10);
        assertThat(provider.fetchLatest()).hasSize(3);
        server.verify();
    }

    @Test
    void retriesWhenJamendoQuietlyReturnsAnEmptyPage() {
        var provider = provider("bollywood", List.of());
        String spurious = "{\"headers\":{\"status\":\"success\",\"results_count\":0},\"results\":[]}";
        server.expect(queryParam("offset", "0")).andRespond(withSuccess(spurious, MediaType.APPLICATION_JSON));
        server.expect(queryParam("offset", "0")).andRespond(withSuccess(page(0, 7), MediaType.APPLICATION_JSON));

        assertThat(provider.fetchLatest()).hasSize(7);
        server.verify();
    }

    @Test
    void aGenuinelyEmptyResultIsNotRetried() {
        var provider = provider("nothing", List.of());
        server.expect(queryParam("offset", "0")).andRespond(withSuccess(
                "{\"headers\":{\"status\":\"success\",\"results_fullcount\":0},\"results\":[]}",
                MediaType.APPLICATION_JSON));

        assertThat(provider.fetchLatest()).isEmpty();
        server.verify();
    }

    @Test
    void stopsPagingOnceTheFullCountIsReached() {
        var provider = provider("bollywood", List.of());
        server.expect(queryParam("offset", "0")).andRespond(withSuccess(
                page(0, 200).replace("100000", "200"), MediaType.APPLICATION_JSON));

        assertThat(provider.fetchLatest()).hasSize(200);
        server.verify();
    }

    @Test
    void sendsEachTagSeparatorEncodedSoJamendoMatchesAnyTag() {
        var provider = provider("indian+bollywood", List.of());
        server.expect(requestTo(containsString("fuzzytags=indian%2Bbollywood")))
                .andRespond(withSuccess(page(0, 0), MediaType.APPLICATION_JSON));

        provider.fetchLatest();

        server.verify();
    }

    @Test
    void failsFastWithoutClientId() {
        var provider = provider(AppProperties.Jamendo.of(true, "https://jamendo.test", "", "bollywood"),
                Clock.systemUTC());

        assertThatThrownBy(provider::fetchLatest).isInstanceOf(IllegalStateException.class);
    }
}
