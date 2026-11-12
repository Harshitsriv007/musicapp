package com.musicapp.acceptance;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CommonSteps {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MusicApi api;

    @Before
    public void resetState() {
        jdbc.execute("DELETE FROM play_history");
        jdbc.execute("DELETE FROM liked_songs");
        jdbc.execute("DELETE FROM user_downloads");
        jdbc.execute("DELETE FROM songs");
        jdbc.execute("DELETE FROM auth_sessions");
        jdbc.execute("DELETE FROM users");
        api.tokens.clear();
        CucumberSpringConfiguration.MUSIC_API.resetAll();
        // MusicBrainz (same WireMock): "no match" unless a scenario says otherwise.
        CucumberSpringConfiguration.MUSIC_API.stubFor(com.github.tomakehurst.wiremock.client.WireMock
                .get(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/ws/2/recording"))
                .atPriority(10)
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.okJson("{\"recordings\":[]}")));
    }
}
