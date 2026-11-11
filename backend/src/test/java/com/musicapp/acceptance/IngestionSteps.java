package com.musicapp.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class IngestionSteps {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    MusicApi api;

    @Autowired
    ObjectMapper json;

    @Given("the free music API offers these {string} songs:")
    public void musicApiOffers(String query, DataTable table) throws Exception {
        List<Map<String, Object>> tracks = table.asMaps().stream()
                .<Map<String, Object>>map(row -> Map.of(
                        "id", row.get("id"),
                        "title", row.get("title"),
                        "genre", "Bollywood",
                        "duration", 240,
                        "release_date", row.get("released") + "T00:00:00Z",
                        "artwork", Map.of("480x480", "https://img.test/" + row.get("id") + ".jpg"),
                        "user", Map.of("name", row.get("artist"))))
                .toList();
        CucumberSpringConfiguration.MUSIC_API.stubFor(get(urlPathEqualTo("/v1/tracks/search"))
                .withQueryParam("query", equalTo(query))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(json.writeValueAsString(Map.of("data", tracks)))));
    }

    @Given("the free music API is down")
    public void musicApiDown() {
        CucumberSpringConfiguration.MUSIC_API.stubFor(get(urlPathEqualTo("/v1/tracks/search"))
                .willReturn(aResponse().withStatus(503)));
    }

    @When("the song ingestion job runs")
    public void ingestionRuns() {
        api.runIngestion();
    }

    @When("the song ingestion job runs again")
    public void ingestionRunsAgain() {
        api.runIngestion();
    }

    @Then("the new songs list eventually shows, newest release first:")
    public void newSongsEventuallyShow(List<String> expected) {
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(api.newSongTitles()).containsExactlyElementsOf(expected));
    }

    @Then("the catalogue eventually contains {int} songs")
    public void catalogueEventuallyContains(int count) {
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(api.catalogueSize()).isEqualTo(count));
    }

    @Then("the catalogue still contains {int} songs")
    public void catalogueStillContains(int count) {
        await().during(Duration.ofSeconds(3)).atMost(TIMEOUT)
                .untilAsserted(() -> assertThat(api.catalogueSize()).isEqualTo(count));
    }

    @Then("the song {string} by {string} is playable with artwork")
    public void songIsPlayable(String title, String artist) {
        var song = api.get("/api/songs?q=" + title.replace(" ", "%20"), "guest").get("content").get(0);
        assertThat(song.get("title").asText()).isEqualTo(title);
        assertThat(song.get("artist").asText()).isEqualTo(artist);
        assertThat(song.get("streamUrl").asText()).contains("/v1/tracks/").endsWith("/stream?app_name=musicapp-test");
        assertThat(song.get("imageUrl").asText()).startsWith("https://img.test/");
    }
}
