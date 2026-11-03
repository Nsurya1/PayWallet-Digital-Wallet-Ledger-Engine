-- ============================================================================
-- PayWallet Schema Migration: V7__password_reset_tokens.sql
-- Password Reset Tokens for self-service credential recovery
-- ============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pwd_reset_tokens_user_token ON password_reset_tokens(user_id, token);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_tokens_expires ON password_reset_tokens(expires_at);
