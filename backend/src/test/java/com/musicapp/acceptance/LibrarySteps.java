package com.musicapp.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicapp.domain.Song;
import com.musicapp.repository.SongRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

public class LibrarySteps {

    @Autowired
    MusicApi api;

    @Autowired
    SongRepository songs;

    @Given("the catalogue contains the songs {string}")
    public void catalogueContains(String titles) {
        for (String title : split(titles)) {
            Song song = new Song("audius", title.toLowerCase().replace(' ', '-'));
            song.setTitle(title);
            song.setArtist("Various");
            song.setStreamUrl("https://stream.test/" + song.getExternalId());
            songs.save(song);
        }
    }

    @When("listener {string} likes {string}")
    public void likes(String user, String title) {
        assertThat(api.send(HttpMethod.PUT, "/api/likes/" + idOf(title), user, null).value()).isEqualTo(204);
    }

    @When("listener {string} unlikes {string}")
    public void unlikes(String user, String title) {
        assertThat(api.send(HttpMethod.DELETE, "/api/likes/" + idOf(title), user, null).value()).isEqualTo(204);
    }

    @When("listener {string} plays {string}")
    public void plays(String user, String title) {
        assertThat(api.send(HttpMethod.POST, "/api/history", user, Map.of("songId", idOf(title))).value()).isEqualTo(201);
    }

    @When("listener {string} clears the play history")
    public void clearsHistory(String user) {
        assertThat(api.send(HttpMethod.DELETE, "/api/history", user, null).value()).isEqualTo(204);
    }

    @Then("the liked songs of {string} are {string}")
    public void likedSongsAre(String user, String titles) {
        assertThat(api.likedTitles(user)).containsExactlyElementsOf(split(titles));
    }

    @Then("the liked songs of {string} are empty")
    public void likedSongsEmpty(String user) {
        assertThat(api.likedTitles(user)).isEmpty();
    }

    @Then("{string} is shown as liked for {string} but not for {string}")
    public void likedFlagPerUser(String title, String user, String otherUser) {
        long id = idOf(title);
        assertThat(api.get("/api/songs/" + id, user).get("liked").asBoolean()).isTrue();
        assertThat(api.get("/api/songs/" + id, otherUser).get("liked").asBoolean()).isFalse();
    }

    @Then("the play history of {string} is {string}")
    public void historyIs(String user, String titles) {
        assertThat(api.historyTitles(user)).containsExactlyElementsOf(split(titles));
    }

    @Then("the play history of {string} is empty")
    public void historyEmpty(String user) {
        assertThat(api.historyTitles(user)).isEmpty();
    }

    @Then("liking an unknown song is rejected as not found")
    public void likingUnknownSong() {
        assertThat(api.send(HttpMethod.PUT, "/api/likes/999999", "riya", null).value()).isEqualTo(404);
    }

    private long idOf(String title) {
        return songs.findAll().stream()
                .filter(s -> s.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No song titled " + title))
                .getId();
    }

    private static List<String> split(String csv) {
        return Arrays.stream(csv.split(",")).map(String::strip).toList();
    }
}
