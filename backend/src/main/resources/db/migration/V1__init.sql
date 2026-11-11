CREATE TABLE songs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    source           VARCHAR(32)   NOT NULL,
    external_id      VARCHAR(128)  NOT NULL,
    title            VARCHAR(512)  NOT NULL,
    artist           VARCHAR(255),
    album            VARCHAR(255),
    genre            VARCHAR(255),
    image_url        VARCHAR(1024),
    stream_url       VARCHAR(1024) NOT NULL,
    duration_seconds INT,
    release_date     DATE,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    CONSTRAINT uk_songs_source_external UNIQUE (source, external_id),
    INDEX idx_songs_newest (release_date, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE play_history (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id   VARCHAR(64) NOT NULL,
    song_id   BIGINT      NOT NULL,
    played_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_history_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE,
    INDEX idx_history_user_played (user_id, played_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE liked_songs (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id  VARCHAR(64) NOT NULL,
    song_id  BIGINT      NOT NULL,
    liked_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_liked_user_song UNIQUE (user_id, song_id),
    CONSTRAINT fk_liked_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE,
    INDEX idx_liked_user_liked (user_id, liked_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
