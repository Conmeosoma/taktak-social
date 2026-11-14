ALTER TABLE moments ADD COLUMN media_type VARCHAR(20) NOT NULL DEFAULT 'PHOTO' CHECK (media_type IN ('PHOTO', 'LIVE_VIDEO'));
ALTER TABLE moments ADD COLUMN video_path TEXT;
ALTER TABLE moments ADD COLUMN video_byte_size BIGINT;
ALTER TABLE moments ADD COLUMN duration_ms INTEGER;
ALTER TABLE moments ADD COLUMN has_audio BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX ix_moments_media_type ON moments(media_type);

