-- Downloads belong to the listener who made them. The audio itself is still stored once per song
-- (songs.download_status / song_audio act as a shared cache), but a listener only sees, and can only
-- remove, the downloads in their own list. Existing downloads have no known owner: their audio stays
-- cached, so downloading them again finishes instantly.
CREATE TABLE user_downloads (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      VARCHAR(64) NOT NULL,
    song_id      BIGINT      NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_download_user_song UNIQUE (user_id, song_id),
    CONSTRAINT fk_download_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE,
    INDEX idx_download_song (song_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sessions now expire a fixed time after login; this index serves the cleanup job.
CREATE INDEX idx_sessions_created ON auth_sessions (created_at);
