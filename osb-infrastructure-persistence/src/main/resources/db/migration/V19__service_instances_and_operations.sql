-- Persist OSB service instances and n8n workflow operation snapshots.

CREATE TABLE IF NOT EXISTS service_instances (
    id                          VARCHAR(128) PRIMARY KEY,
    service_id                  VARCHAR(128) NOT NULL,
    plan_id                     VARCHAR(128) NOT NULL,
    state                       VARCHAR(64)  NOT NULL,
    platform_client_id          VARCHAR(128),
    dashboard_url               VARCHAR(2048),
    parameters                  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    last_operation_id           VARCHAR(128),
    last_operation_state        VARCHAR(64),
    last_operation_description  TEXT,
    last_operation_kind         VARCHAR(32),
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflow_operations (
    id                   VARCHAR(128) PRIMARY KEY,
    status               VARCHAR(32)  NOT NULL,
    dashboard_url        VARCHAR(2048),
    service_id           VARCHAR(128),
    command_json         TEXT,
    poll_last_operation  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
