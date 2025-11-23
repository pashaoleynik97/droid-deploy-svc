-- Table: application
CREATE TABLE application (
    id UUID PRIMARY KEY
);

-- Table: user
CREATE TABLE "user" (
    id UUID PRIMARY KEY,
    login VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'CI', 'CONSUMER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ,
    last_interaction_at TIMESTAMPTZ,
    token_version INTEGER NOT NULL DEFAULT 0
);

-- Table: api_key
CREATE TABLE api_key (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    value_hash TEXT UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('CI', 'CONSUMER')),
    application_id UUID NOT NULL REFERENCES application(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    token_version INTEGER NOT NULL DEFAULT 0
);

-- Indexes for better query performance
CREATE INDEX idx_user_login ON "user"(login);
CREATE INDEX idx_user_role ON "user"(role);
CREATE INDEX idx_user_is_active ON "user"(is_active);
CREATE INDEX idx_api_key_value_hash ON api_key(value_hash);
CREATE INDEX idx_api_key_application_id ON api_key(application_id);
CREATE INDEX idx_api_key_is_active ON api_key(is_active);
