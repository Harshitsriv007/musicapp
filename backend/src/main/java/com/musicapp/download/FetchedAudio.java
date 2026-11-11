package com.musicapp.download;

import java.io.IOException;
import java.io.InputStream;

/** An open audio stream from the external source. */
public record FetchedAudio(InputStream body, String contentType, long contentLength) implements AutoCloseable {
    @Override
    public void close() throws IOException {
        body.close();
    }
}
