-- AUTH-B11: tokens are now stored as SHA-256 hashes; rename column to reflect that
ALTER TABLE password_reset_tokens RENAME COLUMN token TO token_hash;
ALTER INDEX IF EXISTS idx_password_reset_tokens_token RENAME TO idx_password_reset_tokens_token_hash;
