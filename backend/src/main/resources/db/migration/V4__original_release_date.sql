-- Audius only knows when a track was *uploaded*. original_release_date is looked up in
-- MusicBrainz (when a confident match exists) and drives the decade folders.
ALTER TABLE songs
    ADD COLUMN original_release_date DATE,
    ADD COLUMN metadata_checked_at   DATETIME(6);

CREATE INDEX idx_songs_metadata_checked ON songs (metadata_checked_at);
