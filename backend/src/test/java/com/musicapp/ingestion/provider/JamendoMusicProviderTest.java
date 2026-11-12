package com.musicapp.ingestion.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.musicapp.TestFixtures;
import com.musicapp.config.AppProperties;
import com.musicapp.ingestion.SongEvent;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JamendoMusicProviderTest {

    @Test
    void mapsResultsAndSkipsTracksWithoutAudio() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var provider = new JamendoMusicProvider(builder, TestFixtures.props("https://audius.test", List.of("x")));
        server.expect(queryParam("client_id", "cid"))
                .andExpect(queryParam("order", "releasedate_desc"))
                .andExpect(queryParam("fuzzytags", "bollywood"))
                .andRespond(withSuccess("""
                        {"headers":{"status":"success"},"results":[
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
    void failsFastWithoutClientId() {
        var base = TestFixtures.props("https://audius.test", List.of("x"));
        var props = new AppProperties(base.ingestion(), base.kafka(),
                new AppProperties.Providers(base.providers().audius(),
                        new AppProperties.Jamendo(true, "https://jamendo.test", "", "bollywood")),
                base.cors(), base.download(), base.enrichment());
        var provider = new JamendoMusicProvider(RestClient.builder(), props);

        assertThatThrownBy(provider::fetchLatest).isInstanceOf(IllegalStateException.class);
    }
}
