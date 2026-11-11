-- Folder browsing: by year (release_date), by artist, by movie/album.
ALTER TABLE songs ADD COLUMN movie VARCHAR(255);

CREATE INDEX idx_songs_movie ON songs (movie);
CREATE INDEX idx_songs_artist ON songs (artist);
