package com.musicapp.ingestion.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.musicapp.TestFixtures;
import com.musicapp.ingestion.SongEvent;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class AudiusMusicProviderTest {

    private static final String BODY = """
            {"data":[
              {"id":"ANM13","title":"Kesariya","genre":"Bollywood","duration":268,
               "release_date":"2022-07-17T00:00:00Z","created_at":"2022-07-18T10:00:00Z",
               "artwork":{"150x150":"https://img/150.jpg","480x480":"https://img/480.jpg","mirrors":["https://m"]},
               "user":{"name":"Arijit Singh","handle":"arijit"},"is_delete":false,"is_available":true,
               "extra_unknown_field":123},
              {"id":"DEL1","title":"Deleted","is_delete":true},
              {"id":"GATE1","title":"Premium only","is_stream_gated":true},
              {"id":"OLD1","title":"No release date","created_at":"2020-01-02T03:04:05Z","user":{"name":"X"}}
            ]}
            """;

    private MockRestServiceServer server;
    private AudiusMusicProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new AudiusMusicProvider(builder, TestFixtures.props("https://audius.test/", List.of("bollywood")));
    }

    @Test
    void mapsPlayableTracksToSongEvents() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://audius.test/v1/tracks/search")))
                .andExpect(queryParam("query", "bollywood"))
                .andExpect(queryParam("limit", "10"))
                .andExpect(queryParam("app_name", "testapp"))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        List<SongEvent> songs = provider.fetchLatest();

        assertThat(songs).extracting(SongEvent::externalId).containsExactly("ANM13", "OLD1");
        SongEvent kesariya = songs.getFirst();
        assertThat(kesariya.source()).isEqualTo("audius");
        assertThat(kesariya.title()).isEqualTo("Kesariya");
        assertThat(kesariya.artist()).isEqualTo("Arijit Singh");
        assertThat(kesariya.imageUrl()).isEqualTo("https://img/480.jpg");
        assertThat(kesariya.streamUrl()).isEqualTo("https://audius.test/v1/tracks/ANM13/stream?app_name=testapp");
        assertThat(kesariya.durationSeconds()).isEqualTo(268);
        assertThat(kesariya.releaseDate()).isEqualTo(LocalDate.of(2022, 7, 17));
        // falls back to created_at when release_date is missing
        assertThat(songs.get(1).releaseDate()).isEqualTo(LocalDate.of(2020, 1, 2));
        server.verify();
    }

    @Test
    void queriesEveryConfiguredSearchTerm() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new AudiusMusicProvider(builder, TestFixtures.props("https://audius.test", List.of("bollywood", "hindi")));
        server.expect(ExpectedCount.once(), queryParam("query", "bollywood"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), queryParam("query", "hindi"))
                .andRespond(withSuccess("{\"data\":null}", MediaType.APPLICATION_JSON));

        assertThat(provider.fetchLatest()).isEmpty();
        server.verify();
    }

    @Test
    void propagatesServerErrors() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/tracks/search")))
                .andRespond(withServerError());

        assertThatThrownBy(provider::fetchLatest).isInstanceOf(HttpServerErrorException.class);
    }
}
