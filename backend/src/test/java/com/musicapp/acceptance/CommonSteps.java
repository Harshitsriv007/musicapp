package com.musicapp.acceptance;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CommonSteps {

    @Autowired
    JdbcTemplate jdbc;

    @Before
    public void resetState() {
        jdbc.execute("DELETE FROM play_history");
        jdbc.execute("DELETE FROM liked_songs");
        jdbc.execute("DELETE FROM songs");
        CucumberSpringConfiguration.MUSIC_API.resetAll();
    }
}
