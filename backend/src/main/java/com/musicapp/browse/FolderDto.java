package com.musicapp.browse;

/**
 * One folder on the browse screen.
 *
 * @param key      value to filter songs by (year, artist name or movie name)
 * @param imageUrl artwork of one of the songs inside, used as the folder cover
 */
public record FolderDto(String key, String title, long songCount, String imageUrl) {}
