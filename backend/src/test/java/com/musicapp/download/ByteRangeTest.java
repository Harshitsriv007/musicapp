package com.musicapp.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ByteRangeTest {

    @ParameterizedTest
    @CsvSource({
            "bytes=0-99,      0,   99",
            "bytes=100-,      100, 999",
            "bytes=-200,      800, 999",
            "bytes=900-5000,  900, 999",
            "bytes=-5000,     0,   999",
            "'bytes=10-19,50-60', 10, 19"
    })
    void parsesRanges(String header, long start, long end) {
        assertThat(ByteRange.parse(header, 1000)).contains(new ByteRange(start, end));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"items=0-10", "bytes=abc", "bytes=5"})
    void ignoresMissingOrUnsupportedHeaders(String header) {
        assertThat(ByteRange.parse(header, 1000)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"bytes=1000-", "bytes=50-10", "bytes=-0"})
    void rejectsUnsatisfiableRanges(String header) {
        assertThatThrownBy(() -> ByteRange.parse(header, 1000)).isInstanceOf(ByteRange.NotSatisfiableException.class);
    }

    @org.junit.jupiter.api.Test
    void lengthIsInclusive() {
        assertThat(new ByteRange(0, 0).length()).isEqualTo(1);
        assertThat(new ByteRange(10, 19).length()).isEqualTo(10);
    }
}
