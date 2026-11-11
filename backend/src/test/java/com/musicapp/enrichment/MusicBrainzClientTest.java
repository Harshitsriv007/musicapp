package com.musicapp.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.musicapp.TestFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MusicBrainzClientTest {

    private MockRestServiceServer server;
    private MusicBrainzClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MusicBrainzClient(builder, TestFixtures.props("https://a", List.of("q")),
                Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC));
    }

    private void respond(String titleInQuery, String json) {
        server.expect(requestTo(Matchers.startsWith("https://mb.test/ws/2/recording")))
                .andExpect(queryParam("query", "recording%3A%22" + titleInQuery.replace(" ", "%20") + "%22"))
                .andExpect(queryParam("fmt", "json"))
                .andExpect(header("User-Agent", "DhunTest/1.0"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void takesEarliestReleaseOfAConfidentMatch() {
        respond("Tum Hi Ho", """
                {"recordings":[
                  {"title":"Tum Hi Ho","score":100,"first-release-date":"2013-04-05","artist-credit":[{"name":"Arijit Singh"}]},
                  {"title":"Tum Hi Ho","score":98,"first-release-date":"2013-03","artist-credit":[{"name":"Arijit Singh"}]},
                  {"title":"Tum Hi Ho","score":97,"first-release-date":"1999","artist-credit":[{"name":"Someone Else"}]},
                  {"title":"Tum Hi Ho (Unplugged)","score":95,"first-release-date":"2005","artist-credit":[{"name":"Arijit Singh"}]}
                ]}""");

        assertThat(client.findOriginalReleaseDate("Tum Hi Ho", "Arijit Singh")).contains(LocalDate.of(2013, 3, 1));
        server.verify();
    }

    @Test
    void artistCanComeFromTheTitleWhenTheUploaderIsAFanChannel() {
        respond("Lag Ja Gale", """
                {"recordings":[{"title":"Lag Ja Gale","score":100,"first-release-date":"1964","artist-credit":[{"name":"Lata Mangeshkar"}]}]}""");

        assertThat(client.findOriginalReleaseDate("Lag Ja Gale - Lata Mangeshkar", "Old Is Gold Channel"))
                .contains(LocalDate.of(1964, 1, 1));
    }

    @Test
    void noMatchWhenArtistDiffers() {
        respond("Love", """
                {"recordings":[{"title":"Love","score":100,"first-release-date":"1970","artist-credit":[{"name":"John Lennon"}]}]}""");

        assertThat(client.findOriginalReleaseDate("Love", "Mita Gillan Wala")).isEmpty();
    }

    @Test
    void lowScoresAndFutureDatesAreIgnored() {
        respond("Heeriye", """
                {"recordings":[
                  {"title":"Heeriye","score":60,"first-release-date":"1990","artist-credit":[{"name":"Arijit Singh"}]},
                  {"title":"Heeriye","score":100,"first-release-date":"2031-01-01","artist-credit":[{"name":"Arijit Singh"}]}
                ]}""");

        assertThat(client.findOriginalReleaseDate("Heeriye", "Arijit Singh")).isEmpty();
    }

    @Test
    void retriesWhenMusicBrainzIsBusy() {
        server.expect(requestTo(Matchers.startsWith("https://mb.test/ws/2/recording")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable());
        respond("Tum Hi Ho", """
                {"recordings":[{"title":"Tum Hi Ho","score":100,"first-release-date":"2013","artist-credit":[{"name":"Arijit Singh"}]}]}""");

        assertThat(client.findOriginalReleaseDate("Tum Hi Ho", "Arijit Singh")).contains(LocalDate.of(2013, 1, 1));
        server.verify();
    }

    @Test
    void givesUpAfterRepeatedBusyAnswersSoTheJobCanRetryLater() {
        server.expect(org.springframework.test.web.client.ExpectedCount.times(4),
                        requestTo(Matchers.startsWith("https://mb.test/ws/2/recording")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.findOriginalReleaseDate("Tum Hi Ho", "Arijit Singh"))
                .isInstanceOf(org.springframework.web.client.HttpServerErrorException.ServiceUnavailable.class);
        server.verify();
    }

    @Test
    void mashupsNeverCallMusicBrainz() {
        server.expect(never(), requestTo(Matchers.anything()));

        assertThat(client.findOriginalReleaseDate("90s Bollywood Mashup", "NFThunk")).isEmpty();
        server.verify();
    }

    @Test
    void parsesPartialDates() {
        assertThat(MusicBrainzClient.parseDate("1975")).isEqualTo(LocalDate.of(1975, 1, 1));
        assertThat(MusicBrainzClient.parseDate("1975-08")).isEqualTo(LocalDate.of(1975, 8, 1));
        assertThat(MusicBrainzClient.parseDate("1975-08-15")).isEqualTo(LocalDate.of(1975, 8, 15));
        assertThat(MusicBrainzClient.parseDate("")).isNull();
        assertThat(MusicBrainzClient.parseDate("19xx")).isNull();
    }
}
