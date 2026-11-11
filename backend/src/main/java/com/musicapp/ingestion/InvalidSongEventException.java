package com.musicapp.ingestion;

public class InvalidSongEventException extends RuntimeException {
    public InvalidSongEventException(String message) {
        super(message);
    }
}
