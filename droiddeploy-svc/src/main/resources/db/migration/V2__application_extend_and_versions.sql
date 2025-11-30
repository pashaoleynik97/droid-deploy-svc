ALTER TABLE application
    ADD COLUMN name VARCHAR(255) NOT NULL,
    ADD COLUMN bundle_id VARCHAR(255) NOT NULL,
    ADD COLUMN signing_certificate_sha256 VARCHAR(64),
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE UNIQUE INDEX ux_application_bundle_id
    ON application (bundle_id);

CREATE TABLE application_version
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    application_id UUID        NOT NULL,
    version_name   VARCHAR(50),
    version_code   BIGINT,
    stable         BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_application_version_application
        FOREIGN KEY (application_id)
            REFERENCES application (id)
            ON DELETE CASCADE
);

CREATE INDEX ix_application_version_app
    ON application_version (application_id);