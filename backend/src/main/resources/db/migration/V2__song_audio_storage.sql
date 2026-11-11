-- Downloaded audio is stored in MySQL so songs play from our own DB instead of the external API.
ALTER TABLE songs
    ADD COLUMN download_status    VARCHAR(16)  NOT NULL DEFAULT 'NONE',
    ADD COLUMN audio_content_type VARCHAR(64),
    ADD COLUMN audio_size         BIGINT,
    ADD COLUMN downloaded_at      DATETIME(6),
    ADD COLUMN download_error     VARCHAR(512);

CREATE INDEX idx_songs_download_status ON songs (download_status);

-- Audio split into 1 MiB chunks: avoids max_allowed_packet limits and lets HTTP Range
-- requests (seeking) read only the chunks they need.
CREATE TABLE song_audio_chunks (
    song_id BIGINT     NOT NULL,
    seq     INT        NOT NULL,
    data    MEDIUMBLOB NOT NULL,
    PRIMARY KEY (song_id, seq),
    CONSTRAINT fk_chunks_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE
) ENGINE = InnoDB;
