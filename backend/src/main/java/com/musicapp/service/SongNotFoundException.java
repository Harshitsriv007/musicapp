package com.musicapp.service;

public class SongNotFoundException extends RuntimeException {
    public SongNotFoundException(Long id) {
        super("Song " + id + " not found");
    }
}
