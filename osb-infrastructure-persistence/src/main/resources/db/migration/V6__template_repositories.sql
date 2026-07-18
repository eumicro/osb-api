-- First-class text templates with placeholders (independent of Git/K8s/HTTP clients).
-- Workflows reference templates; n8n receives them in the webhook payload for evaluation.
-- Domain tokens use dollar-brace syntax (instanceId, serviceId, ...). Flyway must not
-- treat those as its own placeholders — see quarkus.flyway.placeholder-prefix in osb-api.

CREATE TABLE templates (
    id          VARCHAR(128) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL DEFAULT '',
    kind        VARCHAR(64)  NOT NULL,
    content     TEXT         NOT NULL DEFAULT '',
    enabled     BOOLEAN      NOT NULL
);

CREATE TABLE workflow_templates (
    workflow_id VARCHAR(128) NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    template_id VARCHAR(128) NOT NULL REFERENCES templates (id) ON DELETE RESTRICT,
    PRIMARY KEY (workflow_id, template_id)
);

-- Realtest templates are inserted in V7__realtest_scenarios.sql.
