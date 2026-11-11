package com.musicapp.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class AccountSteps {

    @Autowired
    MusicApi api;

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

    @Then("requests from {string} are rejected until it logs in again")
    public void rejectedAfterLogout(String device) {
        assertThat(api.call(HttpMethod.GET, "/api/likes", device, null).getStatusCode().value()).isEqualTo(401);
        api.tokens.remove(device);
        assertThat(api.call(HttpMethod.GET, "/api/likes", device, null).getStatusCode().value()).isEqualTo(200);
    }
}
