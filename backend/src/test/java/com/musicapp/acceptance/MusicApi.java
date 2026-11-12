package com.musicapp.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Thin REST client used by the step definitions – everything goes through the public HTTP API.
 * A "user" is a device: it sends its device id, plus its bearer token once it has logged in.
 * Listener data needs an account, so a device that has not logged in yet gets its own account
 * (named after the device) the first time it calls a non-auth endpoint.
 */
@Component
public class MusicApi {

    @Autowired
    TestRestTemplate http;

    /** Bearer token per device, set by the account steps. */
    final Map<String, String> tokens = new HashMap<>();

    static final String LISTENER_PASSWORD = "listener-pass";

    public ResponseEntity<JsonNode> call(HttpMethod method, String path, String user, Object body) {
        if (user != null && !tokens.containsKey(user) && !path.startsWith("/api/auth/")) {
            logInAs(user);
        }
        return callWithoutLogin(method, path, user, body);
    }

    /** As {@link #call}, but never logs the device in first. */
    public ResponseEntity<JsonNode> callWithoutLogin(HttpMethod method, String path, String user, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (user != null) {
            headers.set("X-User-Id", user);
            if (tokens.containsKey(user)) {
                headers.setBearerAuth(tokens.get(user));
            }
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private void logInAs(String user) {
        Map<String, String> credentials = Map.of("username", user, "password", LISTENER_PASSWORD);
        ResponseEntity<JsonNode> response = callWithoutLogin(HttpMethod.POST, "/api/auth/signup", user, credentials);
        if (response.getStatusCode().value() == 409) {
            response = callWithoutLogin(HttpMethod.POST, "/api/auth/login", user, credentials);
        }
        assertThat(response.getStatusCode().is2xxSuccessful()).as("log in %s -> %s", user, response.getStatusCode()).isTrue();
        tokens.put(user, response.getBody().get("token").asText());
    }

    public JsonNode get(String path, String user) {
        ResponseEntity<JsonNode> response = call(HttpMethod.GET, path, user, null);
        assertThat(response.getStatusCode().is2xxSuccessful()).as("GET %s -> %s", path, response.getStatusCode()).isTrue();
        return response.getBody();
    }

    public HttpStatusCode send(HttpMethod method, String path, String user, Object body) {
        return call(method, path, user, body).getStatusCode();
    }

    public List<String> newSongTitles() {
        return titles(get("/api/songs/new", null), null);
    }

    public List<String> likedTitles(String user) {
        return titles(get("/api/likes", user).get("content"), "song");
    }

    public List<String> historyTitles(String user) {
        return titles(get("/api/history", user).get("content"), "song");
    }

    public long catalogueSize() {
        return get("/api/songs?size=1", null).get("totalElements").asLong();
    }

    public void runIngestion() {
        assertThat(send(HttpMethod.POST, "/api/admin/ingestion/run", null, Map.of()).value()).isEqualTo(202);
    }

    private static List<String> titles(JsonNode array, String songField) {
        List<String> titles = new ArrayList<>();
        array.forEach(n -> titles.add((songField == null ? n : n.get(songField)).get("title").asText()));
        return titles;
    }
}
