-- Per-user UI config (workspace views, theme, locale, …) as opaque JSON.

CREATE TABLE bff_ui_config (
    user_name  VARCHAR(255) NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    payload    JSONB        NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_name, config_key)
);

CREATE INDEX idx_bff_ui_config_updated ON bff_ui_config (updated_at DESC);
