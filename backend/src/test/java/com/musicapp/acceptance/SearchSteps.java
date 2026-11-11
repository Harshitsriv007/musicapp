package com.musicapp.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchSteps {

    @Autowired
    MusicApi api;

    private JsonNode result;

    @When("I search for {string}")
    public void search(String q) {
        // TestRestTemplate encodes the URL itself (encoding here too would turn "%" into "%2525").
        result = api.get("/api/search?q=" + q, "riya");
    }

    @Then("the found songs are {string}")
    public void foundSongs(String titles) {
        assertThat(names(result.get("songs"), "title")).containsExactlyElementsOf(split(titles));
    }

    @Then("the found songs are empty")
    public void noSongs() {
        assertThat(result.get("songs")).isEmpty();
    }

    @Then("the found artists are {string}")
    public void foundArtists(String names) {
        assertThat(names(result.get("artists"), "title")).containsExactlyElementsOf(split(names));
    }

    @Then("the found movies are {string}")
    public void foundMovies(String names) {
        assertThat(names(result.get("movies"), "title")).containsExactlyElementsOf(split(names));
    }

    private static List<String> names(JsonNode array, String field) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get(field).asText()));
        return out;
    }

    private static List<String> split(String csv) {
        return Arrays.stream(csv.split(", ")).map(s -> s.replace("\\\"", "\"")).toList();
    }
}
