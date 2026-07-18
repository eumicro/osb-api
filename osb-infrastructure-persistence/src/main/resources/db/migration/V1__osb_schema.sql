-- OSB core schema (single Postgres database "osb")

CREATE TABLE catalogs (
    id          VARCHAR(128) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL
);

CREATE TABLE service_offerings (
    id          VARCHAR(128) PRIMARY KEY,
    catalog_id  VARCHAR(128) NOT NULL REFERENCES catalogs (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    bindable    BOOLEAN      NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_service_offerings_catalog ON service_offerings (catalog_id);

CREATE TABLE service_plans (
    id          VARCHAR(128) PRIMARY KEY,
    offering_id VARCHAR(128) NOT NULL REFERENCES service_offerings (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    free        BOOLEAN      NOT NULL,
    bindable    BOOLEAN      NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_service_plans_offering ON service_plans (offering_id);

CREATE TABLE platform_clients (
    id           VARCHAR(128) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    username     VARCHAR(255) NOT NULL UNIQUE,
    catalog_id   VARCHAR(128) NOT NULL REFERENCES catalogs (id) ON DELETE RESTRICT,
    enabled      BOOLEAN      NOT NULL
);

CREATE INDEX idx_platform_clients_catalog ON platform_clients (catalog_id);

CREATE TABLE http_client_instances (
    id                   VARCHAR(128) PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    description          TEXT         NOT NULL DEFAULT '',
    base_url             VARCHAR(1024) NOT NULL,
    auth_type            VARCHAR(64)  NOT NULL,
    username             VARCHAR(255) NOT NULL DEFAULT '',
    secret               TEXT         NOT NULL DEFAULT '',
    oauth_client_id      VARCHAR(255) NOT NULL DEFAULT '',
    oauth_client_secret  TEXT         NOT NULL DEFAULT '',
    well_known_url       VARCHAR(1024) NOT NULL DEFAULT '',
    timeout_seconds      INT          NOT NULL,
    enabled              BOOLEAN      NOT NULL
);

CREATE TABLE kubernetes_client_instances (
    id                      VARCHAR(128) PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT         NOT NULL DEFAULT '',
    api_server_url          VARCHAR(1024) NOT NULL,
    default_namespace       VARCHAR(255) NOT NULL,
    auth_type               VARCHAR(64)  NOT NULL,
    username                VARCHAR(255) NOT NULL DEFAULT '',
    token                   TEXT         NOT NULL DEFAULT '',
    oauth_client_id         VARCHAR(255) NOT NULL DEFAULT '',
    oauth_client_secret     TEXT         NOT NULL DEFAULT '',
    well_known_url          VARCHAR(1024) NOT NULL DEFAULT '',
    insecure_skip_tls_verify BOOLEAN     NOT NULL DEFAULT FALSE,
    timeout_seconds         INT          NOT NULL,
    enabled                 BOOLEAN      NOT NULL
);

CREATE TABLE git_client_instances (
    id             VARCHAR(128) PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    description    TEXT         NOT NULL DEFAULT '',
    remote_url     VARCHAR(1024) NOT NULL,
    default_branch VARCHAR(255) NOT NULL,
    auth_method    VARCHAR(32)  NOT NULL,
    username       VARCHAR(255) NOT NULL DEFAULT '',
    secret         TEXT         NOT NULL,
    passphrase     TEXT         NOT NULL DEFAULT '',
    enabled        BOOLEAN      NOT NULL
);

CREATE TABLE workflow_definitions (
    id                VARCHAR(128) PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    description       TEXT         NOT NULL DEFAULT '',
    kind              VARCHAR(32)  NOT NULL,
    n8n_webhook_path  VARCHAR(512) NOT NULL,
    n8n_workflow_id   VARCHAR(255) NOT NULL DEFAULT '',
    enabled           BOOLEAN      NOT NULL
);

CREATE TABLE workflow_client_types (
    workflow_id VARCHAR(128) NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    client_type VARCHAR(32)  NOT NULL,
    PRIMARY KEY (workflow_id, client_type)
);

CREATE TABLE workflow_http_clients (
    workflow_id    VARCHAR(128) NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    http_client_id VARCHAR(128) NOT NULL REFERENCES http_client_instances (id) ON DELETE RESTRICT,
    PRIMARY KEY (workflow_id, http_client_id)
);

CREATE TABLE workflow_kubernetes_clients (
    workflow_id           VARCHAR(128) NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    kubernetes_client_id VARCHAR(128) NOT NULL REFERENCES kubernetes_client_instances (id) ON DELETE RESTRICT,
    PRIMARY KEY (workflow_id, kubernetes_client_id)
);

CREATE TABLE workflow_git_clients (
    workflow_id   VARCHAR(128) NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    git_client_id VARCHAR(128) NOT NULL REFERENCES git_client_instances (id) ON DELETE RESTRICT,
    PRIMARY KEY (workflow_id, git_client_id)
);
