package com.musicapp.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.musicapp.repository.SongRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class DownloadSteps {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static byte[] originalAudio = new byte[0];

    @Autowired
    MusicApi api;

    @Autowired
    TestRestTemplate http;

    @Autowired
    SongRepository songs;

    @Given("the music API serves {int} bytes of audio for song {string}")
    public void musicApiServesAudio(int size, String externalId) {
        originalAudio = new byte[size];
        new Random(42).nextBytes(originalAudio);
        CucumberSpringConfiguration.MUSIC_API.stubFor(get(urlPathEqualTo("/v1/tracks/" + externalId + "/stream"))
                .willReturn(aResponse().withHeader("Content-Type", "audio/mpeg").withBody(originalAudio)));
    }

    @When("listener {string} downloads {string}")
    public void downloads(String user, String title) {
        assertThat(api.send(HttpMethod.POST, "/api/songs/" + idOf(title) + "/download", user, null).value()).isEqualTo(202);
    }

    @When("listener {string} downloads all liked songs")
    public void downloadsAllLiked(String user) {
        List<Long> ids = api.get("/api/likes", user).findValues("id").stream().map(n -> n.asLong()).toList();
        assertThat(api.send(HttpMethod.POST, "/api/downloads", user, Map.of("songIds", ids)).value()).isEqualTo(202);
    }

    @When("listener {string} removes the download of {string}")
    public void removesDownload(String user, String title) {
        assertThat(api.send(HttpMethod.DELETE, "/api/songs/" + idOf(title) + "/download", user, null).value()).isEqualTo(204);
    }

    @Then("{string} is eventually downloaded")
    public void eventuallyDownloaded(String title) {
        eventuallyHasStatus(title, "DOWNLOADED");
        assertThat(song(title).get("streamUrl").asText()).isEqualTo("/api/songs/" + idOf(title) + "/audio");
    }

    @Then("{string} eventually has download status {string}")
    public void eventuallyHasStatus(String title, String status) {
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(song(title).get("downloadStatus").asText()).isEqualTo(status));
    }

    @Then("{string} is not downloaded")
    public void notDownloaded(String title) {
        assertThat(song(title).get("downloadStatus").asText()).isEqualTo("NONE");
        assertThat(song(title).get("streamUrl").asText()).contains("/v1/tracks/");
    }

    @Then("{string} is not downloaded for {string}")
    public void notDownloadedFor(String title, String user) {
        JsonNode song = api.get("/api/songs/" + idOf(title), user);
        assertThat(song.get("downloadStatus").asText()).isEqualTo("NONE");
        assertThat(song.get("streamUrl").asText()).contains("/v1/tracks/");
    }

    @Then("{string} is downloaded for {string}")
    public void downloadedFor(String title, String user) {
        assertThat(api.get("/api/songs/" + idOf(title), user).get("downloadStatus").asText()).isEqualTo("DOWNLOADED");
    }

    @Then("the downloaded songs of {string} are empty")
    public void downloadedSongsEmpty(String user) {
        assertThat(api.get("/api/songs?downloaded=true", user).get("content")).isEmpty();
    }

    @Then("the audio of {string} played from our API matches the original")
    public void audioMatches(String title) {
        ResponseEntity<byte[]> response = audio(title, null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("audio/mpeg");
        assertThat(response.getBody()).isEqualTo(originalAudio);
    }

    @Then("seeking in {string} returns only the requested bytes")
    public void seeking(String title) {
        // crosses the boundary between the 1st and 2nd 1 MiB chunk
        long start = 1_048_000;
        long end = 1_049_999;
        ResponseEntity<byte[]> response = audio(title, "bytes=" + start + "-" + end);
        assertThat(response.getStatusCode().value()).isEqualTo(206);
        assertThat(response.getHeaders().getFirst("Content-Range")).isEqualTo("bytes " + start + "-" + end + "/" + originalAudio.length);
        assertThat(response.getBody()).isEqualTo(Arrays.copyOfRange(originalAudio, (int) start, (int) end + 1));
    }

    @Then("{string} appears in the downloaded songs")
    public void appearsInDownloaded(String title) {
        var content = api.get("/api/songs?downloaded=true", "riya").get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("title").asText()).isEqualTo(title);
    }

    @When("the audio of {string} is played {int} more times")
    public void playedMoreTimes(String title, int times) {
        for (int i = 0; i < times; i++) {
            assertThat(audio(title, null).getStatusCode().value()).isEqualTo(200);
        }
    }

    @Then("the music API served the audio of song {string} only once")
    public void apiCalledOnce(String externalId) {
        CucumberSpringConfiguration.MUSIC_API.verify(exactly(1),
                getRequestedFor(urlPathEqualTo("/v1/tracks/" + externalId + "/stream")));
    }

    @Then("the audio of {string} is no longer served")
    public void noLongerServed(String title) {
        assertThat(audio(title, null).getStatusCode().value()).isEqualTo(404);
    }

    private ResponseEntity<byte[]> audio(String title, String range) {
        HttpHeaders headers = new HttpHeaders();
        if (range != null) {
            headers.set(HttpHeaders.RANGE, range);
        }
        return http.exchange("/api/songs/" + idOf(title) + "/audio", HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    private JsonNode song(String title) {
        return api.get("/api/songs/" + idOf(title), "riya");
    }

    private long idOf(String title) {
        return songs.findAll().stream().filter(s -> s.getTitle().equals(title)).findFirst()
                .orElseThrow(() -> new AssertionError("No song " + title)).getId();
    }
}
