package com.musicapp.ingestion.provider;

import com.musicapp.ingestion.SongEvent;
import java.util.List;

/** A free music catalogue the ingestion job can pull songs from. */
public interface MusicProvider {

    String name();

    List<SongEvent> fetchLatest();
}
