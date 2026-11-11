package com.musicapp.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MovieExtractorTest {

    @ParameterizedTest
    @CsvSource(delimiterString = "=>", value = {
            "Kesariya (From \"Brahmastra\")                    => Brahmastra",
            "Chaleya (From “Jawan”) - Hindi                     => Jawan",
            "Tum Hi Ho [From the movie Aashiqui 2]             => Aashiqui 2",
            "Apna Bana Le (from the film \"Bhediya\")           => Bhediya",
            "Rona Chadita | Movie: Mel Karaade Rabba            => Mel Karaade Rabba",
            "Ghoomar - Film - Padmaavat                         => Padmaavat"
    })
    void readsMovieFromTitle(String title, String movie) {
        assertThat(MovieExtractor.extract(title, null)).isEqualTo(movie);
    }

    @Test
    void titleWinsOverAlbum() {
        assertThat(MovieExtractor.extract("Kesariya (From \"Brahmastra\")", "Hits 2022")).isEqualTo("Brahmastra");
    }

    @Test
    void fallsBackToAlbum() {
        assertThat(MovieExtractor.extract("Kesariya", "  Brahmastra   OST ")).isEqualTo("Brahmastra OST");
    }

    @Test
    void unknownWhenNothingMatches() {
        assertThat(MovieExtractor.extract("Lofi Love Mashup | Non-Stop Bollywood", null)).isNull();
        assertThat(MovieExtractor.extract(null, " ")).isNull();
    }
}
