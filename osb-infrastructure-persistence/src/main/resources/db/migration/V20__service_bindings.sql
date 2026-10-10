-- Persist OSB service bindings.

CREATE TABLE IF NOT EXISTS service_bindings (
    id                          VARCHAR(128) NOT NULL,
    instance_id                 VARCHAR(128) NOT NULL REFERENCES service_instances(id) ON DELETE CASCADE,
    service_id                  VARCHAR(128) NOT NULL,
    plan_id                     VARCHAR(128) NOT NULL,
    platform_client_id          VARCHAR(128),
    state                       VARCHAR(64)  NOT NULL,
    credentials                 JSONB        NOT NULL DEFAULT '{}'::jsonb,
    parameters                  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    last_operation_id           VARCHAR(128),
    last_operation_state        VARCHAR(64),
    last_operation_description  TEXT,
    last_operation_kind         VARCHAR(32),
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (instance_id, id)
);

CREATE INDEX IF NOT EXISTS idx_service_bindings_instance ON service_bindings (instance_id);
