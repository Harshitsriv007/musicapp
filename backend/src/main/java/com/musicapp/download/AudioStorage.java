package com.musicapp.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stores song audio in MySQL as 1 MiB chunks (table {@code song_audio_chunks}).
 * Reading a byte range only loads the chunks that overlap it, so seeking is cheap.
 */
@Component
public class AudioStorage {

    public static final int CHUNK_SIZE = 1024 * 1024;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public AudioStorage(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /** Replaces the stored audio of a song. All-or-nothing: a failure leaves no partial audio. */
    public long store(long songId, InputStream in, long maxBytes) {
        Long size = tx.execute(status -> {
            jdbc.update("DELETE FROM song_audio_chunks WHERE song_id = ?", songId);
            byte[] buffer = new byte[CHUNK_SIZE];
            long total = 0;
            int seq = 0;
            while (true) {
                int n = readChunk(in, buffer);
                if (n == 0) {
                    break;
                }
                total += n;
                if (total > maxBytes) {
                    throw new AudioTooLargeException(maxBytes);
                }
                jdbc.update("INSERT INTO song_audio_chunks (song_id, seq, data) VALUES (?, ?, ?)",
                        songId, seq++, Arrays.copyOf(buffer, n));
                if (n < CHUNK_SIZE) {
                    break; // readNBytes only returns short at end of stream
                }
            }
            if (total == 0) {
                throw new IllegalStateException("Audio source returned an empty file");
            }
            return total;
        });
        return size == null ? 0 : size;
    }

    /** Writes bytes {@code start..endInclusive} of the stored audio to {@code out}. */
    public void write(long songId, long start, long endInclusive, OutputStream out) throws IOException {
        long first = start / CHUNK_SIZE;
        long last = endInclusive / CHUNK_SIZE;
        for (long seq = first; seq <= last; seq++) {
            byte[] data = jdbc.queryForObject(
                    "SELECT data FROM song_audio_chunks WHERE song_id = ? AND seq = ?", byte[].class, songId, seq);
            if (data == null) {
                throw new IOException("Missing audio chunk " + seq + " for song " + songId);
            }
            long chunkStart = seq * CHUNK_SIZE;
            int from = (int) Math.max(0, start - chunkStart);
            int to = (int) Math.min(data.length, endInclusive - chunkStart + 1);
            out.write(data, from, to - from);
        }
    }

    public void delete(long songId) {
        jdbc.update("DELETE FROM song_audio_chunks WHERE song_id = ?", songId);
    }

    private static int readChunk(InputStream in, byte[] buffer) {
        try {
            return in.readNBytes(buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
