package com.musicapp.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class AccountSteps {

    @Autowired
    MusicApi api;

    @Autowired
    JdbcTemplate jdbc;

    @When("{string} signs up as {string} with password {string}")
    public void signsUp(String device, String username, String password) {
        ResponseEntity<JsonNode> response = api.call(HttpMethod.POST, "/api/auth/signup", device,
                Map.of("username", username, "password", password));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        api.tokens.put(device, response.getBody().get("token").asText());
    }

    @When("{string} logs in as {string} with password {string}")
    public void logsIn(String device, String username, String password) {
        ResponseEntity<JsonNode> response = api.call(HttpMethod.POST, "/api/auth/login", device,
                Map.of("username", username, "password", password));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        api.tokens.put(device, response.getBody().get("token").asText());
    }

    @When("{string} resets the password of {string} to {string}")
    public void resetsPassword(String device, String username, String password) {
        ResponseEntity<JsonNode> response = api.call(HttpMethod.POST, "/api/auth/reset-password", device,
                Map.of("username", username, "newPassword", password));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        api.tokens.put(device, response.getBody().get("token").asText());
    }

    @When("{string} logs out")
    public void logsOut(String device) {
        assertThat(api.send(HttpMethod.POST, "/api/auth/logout", device, null).value()).isEqualTo(204);
    }

    @Then("signing up as {string} is rejected as a conflict")
    public void signUpConflict(String username) {
        assertThat(api.send(HttpMethod.POST, "/api/auth/signup", "other",
                Map.of("username", username, "password", "secret-123")).value()).isEqualTo(409);
    }

    @Then("logging in as {string} with password {string} is rejected")
    public void loginRejected(String username, String password) {
        assertThat(api.send(HttpMethod.POST, "/api/auth/login", "other",
                Map.of("username", username, "password", password)).value()).isEqualTo(401);
    }

    @When("{int} hours pass since {string} logged in")
    public void timePasses(int hours, String device) {
        // Ages every session; only the one device is logged in when this step is used.
        jdbc.update("UPDATE auth_sessions SET created_at = created_at - INTERVAL ? HOUR", hours);
    }

    @Then("requests from {string} are rejected until it logs in again")
    public void rejectedUntilLogin(String device) {
        assertThat(api.call(HttpMethod.GET, "/api/likes", device, null).getStatusCode().value()).isEqualTo(401);
        api.tokens.remove(device);
        // Without a token there is no anonymous fallback: listener data always needs a login.
        assertThat(api.callWithoutLogin(HttpMethod.GET, "/api/likes", device, null).getStatusCode().value()).isEqualTo(401);
    }

    @Then("listener data of {string} is rejected without a login")
    public void rejectedWithoutLogin(String device) {
        for (String path : new String[] {"/api/likes", "/api/history", "/api/songs?downloaded=true&size=1"}) {
            int status = api.callWithoutLogin(HttpMethod.GET, path, device, null).getStatusCode().value();
            if (path.startsWith("/api/songs")) {
                // the catalogue is public, but a visitor has no downloads
                assertThat(api.callWithoutLogin(HttpMethod.GET, path, device, null).getBody().get("content")).isEmpty();
            } else {
                assertThat(status).as(path).isEqualTo(401);
            }
        }
        assertThat(api.callWithoutLogin(HttpMethod.PUT, "/api/likes/1", device, null).getStatusCode().value()).isEqualTo(401);
        assertThat(api.callWithoutLogin(HttpMethod.POST, "/api/songs/1/download", device, null).getStatusCode().value()).isEqualTo(401);
    }

    @Then("{string} can still browse the catalogue without logging in")
    public void catalogueIsPublic(String device) {
        assertThat(api.callWithoutLogin(HttpMethod.GET, "/api/songs/new", null, null).getStatusCode().value()).isEqualTo(200);
    }
}
