-- Credit and licence details some providers (Jamendo) require us to show, and whether the
-- provider lets us store the audio. Existing songs (Audius) keep downloads allowed.
ALTER TABLE songs
    ADD COLUMN share_url        VARCHAR(1024) NULL,
    ADD COLUMN license_url      VARCHAR(1024) NULL,
    ADD COLUMN download_allowed BOOLEAN       NOT NULL DEFAULT TRUE;
