-- Accounts let a listener see the same likes and history on every device.
-- Likes / history keep using the existing user_id column: an account's rows are stored
-- under "acct_<id>", anonymous devices keep their random id.
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Login sessions. Only the SHA-256 of the token is stored, so a DB leak does not leak sessions.
CREATE TABLE auth_sessions (
    token_hash   CHAR(64)    NOT NULL PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    last_used_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_sessions_user (user_id)
) ENGINE = InnoDB;
