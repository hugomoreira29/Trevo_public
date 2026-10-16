ALTER TABLE tryon_images
    ADD COLUMN model_identifier VARCHAR(255) NOT NULL DEFAULT 'default';

CREATE INDEX idx_tryon_images_usage_model_generated_at
    ON tryon_images (usage_id, model_identifier, generated_at DESC);
