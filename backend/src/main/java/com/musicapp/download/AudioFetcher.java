package com.musicapp.download;

import java.io.IOException;

public interface AudioFetcher {

    /** Opens the audio at {@code url}, following redirects. Caller closes the result. */
    FetchedAudio open(String url) throws IOException, InterruptedException;
}
