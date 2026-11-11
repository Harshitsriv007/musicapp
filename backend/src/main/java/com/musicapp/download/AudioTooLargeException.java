package com.musicapp.download;

public class AudioTooLargeException extends RuntimeException {
    public AudioTooLargeException(long maxBytes) {
        super(String.format(java.util.Locale.ROOT, "Audio is larger than the %.1f MB limit", maxBytes / (1024.0 * 1024.0)));
    }
}
