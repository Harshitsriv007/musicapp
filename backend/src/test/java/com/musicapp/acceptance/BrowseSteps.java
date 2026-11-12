package com.musicapp.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

public class BrowseSteps {

    @Autowired
    MusicApi api;

    @Autowired
    ObjectMapper json;

    @Then("the {string} folders are:")
    public void foldersAre(String type, DataTable expected) {
        List<List<String>> actual = new ArrayList<>();
        api.get("/api/browse/" + type, "riya").forEach(f -> actual.add(List.of(f.get("title").asText(), f.get("songCount").asText())));
        assertThat(actual).isEqualTo(expected.cells().subList(1, expected.height()));
    }

    @Then("the {string} folder {string} contains {string}")
    public void folderContains(String filter, String key, String titles) {
        List<String> actual = new ArrayList<>();
        folderSongs(filter, key).forEach(s -> actual.add(s.get("title").asText()));
        List<String> expected = Arrays.stream(titles.split(", ")).map(t -> t.replace("\\\"", "\"")).toList();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Given("MusicBrainz knows {string} by {string} was first released in {string}")
    public void musicBrainzKnows(String title, String artist, String date) throws Exception {
        String body = json.writeValueAsString(Map.of("recordings", List.of(Map.of(
                "title", title, "score", 100, "first-release-date", date,
                "artist-credit", List.of(Map.of("name", artist))))));
        CucumberSpringConfiguration.MUSIC_API.stubFor(get(urlPathEqualTo("/ws/2/recording"))
                .withQueryParam("query", containing("\"" + title + "\""))
                .atPriority(1)
                .willReturn(okJson(body)));
    }

    @When("the release-year lookup runs")
    public void releaseYearLookupRuns() {
        assertThat(api.send(HttpMethod.POST, "/api/admin/enrichment/run", null, Map.of()).value()).isEqualTo(202);
    }

    @Then("the {string} folder {string} eventually contains {string}")
    public void folderEventuallyContains(String filter, String key, String titles) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> folderContains(filter, key, titles));
    }

    @When("listener {string} likes every song in the {string} folder {string}")
    public void likeFolder(String user, String filter, String key) {
        setFolderLiked(user, filter, key, true);
    }

    @When("listener {string} unlikes every song in the {string} folder {string}")
    public void unlikeFolder(String user, String filter, String key) {
        setFolderLiked(user, filter, key, false);
    }

    @Then("the liked songs of {string} contain {int} songs")
    public void likedCount(String user, int count) {
        assertThat(api.likedTitles(user)).hasSize(count);
    }

    private void setFolderLiked(String user, String filter, String key, boolean liked) {
        List<Long> ids = new ArrayList<>();
        folderSongs(filter, key).forEach(s -> ids.add(s.get("id").asLong()));
        assertThat(api.send(HttpMethod.POST, "/api/likes/batch", user, Map.of("songIds", ids, "liked", liked)).value())
                .isEqualTo(204);
    }

    private JsonNode folderSongs(String filter, String key) {
        return api.get("/api/songs?size=100&" + filter + "=" + URLEncoder.encode(key, StandardCharsets.UTF_8), "riya")
                .get("content");
    }
}
