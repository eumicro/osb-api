-- Realtest scenarios (three offerings + workflows + templates).
-- 1) git-file-store  → Gitea file Test-<instanceId>.txt
-- 2) redis-cache     → Redis Deployment in namespace <instanceId>
-- 3) keycloak-realm  → Keycloak Admin API realm osb-<instanceId>
-- Domain placeholders use dollar-brace syntax. Flyway placeholder prefix/suffix
-- are configured in osb-api (quarkus.flyway.placeholder-prefix) so they do not clash.

CREATE TABLE offering_workflows (
    service_id  VARCHAR(128) NOT NULL REFERENCES service_offerings (id) ON DELETE CASCADE,
    kind        VARCHAR(32)  NOT NULL,
    workflow_id VARCHAR(128) NOT NULL REFERENCES workflow_definitions (id) ON DELETE RESTRICT,
    PRIMARY KEY (service_id, kind)
);

CREATE INDEX idx_offering_workflows_workflow ON offering_workflows (workflow_id);

-- ---------------------------------------------------------------------------
-- Clients: Keycloak Admin API (Git/K8s already seeded in V2/V5)
-- ---------------------------------------------------------------------------
INSERT INTO http_client_instances (
    id, name, description, base_url, auth_type, username, secret,
    oauth_client_id, oauth_client_secret, well_known_url, timeout_seconds, enabled
) VALUES (
    'http-keycloak-admin',
    'Keycloak Admin API',
    'Local Keycloak master admin (password grant via admin-cli).',
    'http://localhost:8180',
    'BASIC',
    'admin',
    'admin',
    'admin-cli',
    '',
    'http://localhost:8180/realms/master/.well-known/openid-configuration',
    30,
    TRUE
);

-- ---------------------------------------------------------------------------
-- Catalog offerings
-- ---------------------------------------------------------------------------
INSERT INTO service_offerings (id, catalog_id, name, description, bindable, sort_order) VALUES
    ('git-file-store', 'default', 'git-file-store',
     'Provision writes Test-<instanceId>.txt into Gitea; status checks the file; deprovision deletes it.',
     TRUE, 0),
    ('keycloak-realm', 'default', 'keycloak-realm',
     'Provision creates a Keycloak realm osb-<instanceId> via Admin API; status GETs it; deprovision deletes it.',
     TRUE, 1),
    ('redis-cache', 'k8s-ops', 'redis-cache',
     'Provision applies a Redis Deployment in namespace <instanceId>; status GETs it; deprovision deletes it.',
     TRUE, 0);

INSERT INTO service_plans (id, offering_id, name, description, free, bindable, sort_order, schemas, parameters_ui_schema) VALUES
(
    'git-file-store-default', 'git-file-store', 'default',
    'Commit instance metadata file to Gitea',
    TRUE, TRUE, 0,
    '{
      "service_instance": {
        "create": {
          "parameters": {
            "type": "object",
            "title": "Git file parameters",
            "properties": {
              "note": { "type": "string", "title": "Optional note", "default": "" }
            }
          }
        }
      }
    }'::jsonb,
    '{
      "type": "VerticalLayout",
      "elements": [
        { "type": "Control", "scope": "#/properties/note" }
      ]
    }'::jsonb
),
(
    'keycloak-realm-default', 'keycloak-realm', 'default',
    'Create an isolated Keycloak realm',
    TRUE, TRUE, 0,
    '{
      "service_instance": {
        "create": {
          "parameters": {
            "type": "object",
            "title": "Realm parameters",
            "properties": {
              "displayName": {
                "type": "string",
                "title": "Display name",
                "default": "OSB Realm"
              },
              "adminUsername": {
                "type": "string",
                "title": "Realm admin username",
                "minLength": 1,
                "default": "realm-admin"
              },
              "adminPassword": {
                "type": "string",
                "title": "Realm admin password",
                "minLength": 1,
                "default": "realm-admin"
              },
              "adminPasswordTemporary": {
                "type": "boolean",
                "title": "Temporary password (change on first login)",
                "default": true
              }
            },
            "required": ["adminUsername", "adminPassword"]
          }
        }
      }
    }'::jsonb,
    '{
      "type": "VerticalLayout",
      "elements": [
        { "type": "Control", "scope": "#/properties/displayName" },
        { "type": "Control", "scope": "#/properties/adminUsername" },
        { "type": "Control", "scope": "#/properties/adminPassword" },
        { "type": "Control", "scope": "#/properties/adminPasswordTemporary" }
      ]
    }'::jsonb
),
(
    'redis-cache-default', 'redis-cache', 'default',
    'Redis 7 Deployment in a dedicated namespace',
    TRUE, TRUE, 0,
    '{
      "service_instance": {
        "create": {
          "parameters": {
            "type": "object",
            "title": "Redis parameters",
            "properties": {
              "memory_mb": {
                "type": "integer",
                "title": "Memory limit (Mi)",
                "minimum": 64,
                "default": 128
              }
            }
          }
        }
      }
    }'::jsonb,
    '{
      "type": "VerticalLayout",
      "elements": [
        { "type": "Control", "scope": "#/properties/memory_mb" }
      ]
    }'::jsonb
);

-- ---------------------------------------------------------------------------
-- Templates
-- ---------------------------------------------------------------------------
INSERT INTO templates (id, name, description, kind, content, enabled) VALUES
(
    'tpl-git-instance-file',
    'Git Instance File',
    'Upsert/status/delete Test-<instanceId>.txt with catalog/plan/offering details.',
    'GIT_COMMAND',
    E'{"path":"Test-${instanceId}.txt","content":"OSB Realtest instance file\\ninstanceId: ${instanceId}\\nserviceId: ${serviceId}\\nplanId: ${planId}\\nnote: ${parameters.note}\\n","message":"OSB git instance ${instanceId}"}\n',
    TRUE
),
(
    'tpl-git-bind-file',
    'Git Binding Marker',
    'Binding marker file alongside the instance file.',
    'GIT_COMMAND',
    E'{"path":"Test-${instanceId}-bound.txt","content":"OSB binding marker\\ninstanceId: ${instanceId}\\nserviceId: ${serviceId}\\nplanId: ${planId}\\n","message":"OSB bind ${instanceId}"}\n',
    TRUE
),
(
    'tpl-redis-cache',
    'Redis Cache Deployment',
    'Redis + Redis Commander UI; applied into namespace = instanceId. UI: kubectl -n <instanceId> port-forward svc/redis-ui 8081:8081',
    'KUBERNETES_RESOURCE',
    E'apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: redis\n  labels:\n    app: redis\n    osb-instance-id: ${instanceId}\nspec:\n  replicas: 1\n  selector:\n    matchLabels:\n      app: redis\n      osb-instance-id: ${instanceId}\n  template:\n    metadata:\n      labels:\n        app: redis\n        osb-instance-id: ${instanceId}\n    spec:\n      containers:\n        - name: redis\n          image: redis:7-alpine\n          ports:\n            - containerPort: 6379\n          resources:\n            limits:\n              memory: ${parameters.memory_mb}Mi\n---\napiVersion: v1\nkind: Service\nmetadata:\n  name: redis\n  labels:\n    app: redis\n    osb-instance-id: ${instanceId}\nspec:\n  selector:\n    app: redis\n    osb-instance-id: ${instanceId}\n  ports:\n    - port: 6379\n      targetPort: 6379\n---\napiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: redis-ui\n  labels:\n    app: redis-ui\n    osb-instance-id: ${instanceId}\nspec:\n  replicas: 1\n  selector:\n    matchLabels:\n      app: redis-ui\n      osb-instance-id: ${instanceId}\n  template:\n    metadata:\n      labels:\n        app: redis-ui\n        osb-instance-id: ${instanceId}\n    spec:\n      containers:\n        - name: redis-commander\n          image: rediscommander/redis-commander:latest\n          env:\n            - name: REDIS_HOSTS\n              value: local:redis:6379\n          ports:\n            - containerPort: 8081\n---\napiVersion: v1\nkind: Service\nmetadata:\n  name: redis-ui\n  labels:\n    app: redis-ui\n    osb-instance-id: ${instanceId}\nspec:\n  selector:\n    app: redis-ui\n    osb-instance-id: ${instanceId}\n  ports:\n    - port: 8081\n      targetPort: 8081\n',
    TRUE
),
(
    'tpl-redis-bind-secret',
    'Redis Binding Secret',
    'Opaque Secret with redis connection hints for bind/unbind/get-binding.',
    'KUBERNETES_RESOURCE',
    E'apiVersion: v1\nkind: Secret\nmetadata:\n  name: redis-binding\n  labels:\n    app: redis\n    osb-instance-id: ${instanceId}\ntype: Opaque\nstringData:\n  host: redis.${instanceId}.svc.cluster.local\n  port: \"6379\"\n  uri: redis://redis.${instanceId}.svc.cluster.local:6379\n',
    TRUE
),
(
    'tpl-kc-realm-create',
    'Keycloak Create Realm',
    'POST Admin API create realm osb-<instanceId> with realm-admin user from parameters.',
    'HTTP_REQUEST',
    E'{"method":"POST","path":"/admin/realms","body":{"realm":"osb-${instanceId}","enabled":true,"displayName":"${parameters.displayName}","verifyEmail":false,"loginWithEmailAllowed":true,"registrationAllowed":false,"attributes":{"osb.serviceId":"${serviceId}","osb.planId":"${planId}","osb.instanceId":"${instanceId}"},"users":[{"username":"${parameters.adminUsername}","firstName":"${parameters.adminUsername}","lastName":"Admin","email":"${parameters.adminUsername}@localhost","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"${parameters.adminPassword}","temporary":"${parameters.adminPasswordTemporary}"}],"clientRoles":{"realm-management":["realm-admin"]}}]}}\n',
    TRUE
),
(
    'tpl-kc-realm-get',
    'Keycloak Get Realm',
    'GET Admin API realm (status / last operation / get instance).',
    'HTTP_REQUEST',
    E'{"method":"GET","path":"/admin/realms/osb-${instanceId}"}\n',
    TRUE
),
(
    'tpl-kc-realm-update',
    'Keycloak Update Realm',
    'PUT Admin API realm display name / attributes.',
    'HTTP_REQUEST',
    E'{"method":"PUT","path":"/admin/realms/osb-${instanceId}","body":{"realm":"osb-${instanceId}","enabled":true,"displayName":"${parameters.displayName}","attributes":{"osb.serviceId":"${serviceId}","osb.planId":"${planId}","osb.instanceId":"${instanceId}","osb.updated":"true"}}}\n',
    TRUE
),
(
    'tpl-kc-realm-delete',
    'Keycloak Delete Realm',
    'DELETE Admin API realm.',
    'HTTP_REQUEST',
    E'{"method":"DELETE","path":"/admin/realms/osb-${instanceId}"}\n',
    TRUE
),
(
    'tpl-kc-client-create',
    'Keycloak Create Broker Client',
    'Create a confidential client in the instance realm (bind).',
    'HTTP_REQUEST',
    E'{"method":"POST","path":"/admin/realms/osb-${instanceId}/clients","body":{"clientId":"osb-binding-${instanceId}","name":"OSB Binding","enabled":true,"protocol":"openid-connect","publicClient":false,"secret":"osb-binding-secret","serviceAccountsEnabled":true,"standardFlowEnabled":false,"directAccessGrantsEnabled":false}}\n',
    TRUE
),
(
    'tpl-kc-client-get',
    'Keycloak Get Broker Client',
    'List/filter clients for binding status (by clientId query).',
    'HTTP_REQUEST',
    E'{"method":"GET","path":"/admin/realms/osb-${instanceId}/clients","query":{"clientId":"osb-binding-${instanceId}"}}\n',
    TRUE
),
(
    'tpl-kc-client-delete',
    'Keycloak Binding Client Check (unbind)',
    'Admin API client delete needs the internal UUID; seed unbind GETs the client by clientId to confirm bind state.',
    'HTTP_REQUEST',
    E'{"method":"GET","path":"/admin/realms/osb-${instanceId}/clients","query":{"clientId":"osb-binding-${instanceId}"}}\n',
    TRUE
);

-- ---------------------------------------------------------------------------
-- Workflow definitions (per scenario × OSB kind)
-- ---------------------------------------------------------------------------
INSERT INTO workflow_definitions (
    id, name, description, kind, n8n_webhook_path, n8n_workflow_id, enabled
) VALUES
-- Git
('wf-git-provision', 'Git Provision', 'Commit Test-<instanceId>.txt', 'PROVISION', '/webhook/osb-git-provision', 'osbWfGitProvision', TRUE),
('wf-git-deprovision', 'Git Deprovision', 'Delete Test-<instanceId>.txt', 'DEPROVISION', '/webhook/osb-git-deprovision', 'osbWfGitDeprovision', TRUE),
('wf-git-update', 'Git Update', 'Rewrite instance file', 'UPDATE', '/webhook/osb-git-update', 'osbWfGitUpdate', TRUE),
('wf-git-bind', 'Git Bind', 'Create binding marker file', 'BIND', '/webhook/osb-git-bind', 'osbWfGitBind', TRUE),
('wf-git-unbind', 'Git Unbind', 'Delete binding marker file', 'UNBIND', '/webhook/osb-git-unbind', 'osbWfGitUnbind', TRUE),
('wf-git-get-instance', 'Git Get Instance', 'Checkout/status instance file', 'GET_INSTANCE', '/webhook/osb-git-get-instance', 'osbWfGitGetInstance', TRUE),
('wf-git-get-binding', 'Git Get Binding', 'Status binding marker', 'GET_BINDING', '/webhook/osb-git-get-binding', 'osbWfGitGetBinding', TRUE),
('wf-git-instance-last-op', 'Git Instance Last Op', 'Status instance file as last operation', 'INSTANCE_LAST_OPERATION', '/webhook/osb-git-instance-last-operation', 'osbWfGitInstanceLastOp', TRUE),
('wf-git-binding-last-op', 'Git Binding Last Op', 'Status binding marker as last operation', 'BINDING_LAST_OPERATION', '/webhook/osb-git-binding-last-operation', 'osbWfGitBindingLastOp', TRUE),
-- Redis
('wf-redis-provision', 'Redis Provision', 'Apply Redis in namespace instanceId', 'PROVISION', '/webhook/osb-redis-provision', 'osbWfRedisProvision', TRUE),
('wf-redis-deprovision', 'Redis Deprovision', 'Delete Redis resources', 'DEPROVISION', '/webhook/osb-redis-deprovision', 'osbWfRedisDeprovision', TRUE),
('wf-redis-update', 'Redis Update', 'Re-apply Redis (e.g. memory)', 'UPDATE', '/webhook/osb-redis-update', 'osbWfRedisUpdate', TRUE),
('wf-redis-bind', 'Redis Bind', 'Create binding Secret', 'BIND', '/webhook/osb-redis-bind', 'osbWfRedisBind', TRUE),
('wf-redis-unbind', 'Redis Unbind', 'Delete binding Secret', 'UNBIND', '/webhook/osb-redis-unbind', 'osbWfRedisUnbind', TRUE),
('wf-redis-get-instance', 'Redis Get Instance', 'GET Redis resources', 'GET_INSTANCE', '/webhook/osb-redis-get-instance', 'osbWfRedisGetInstance', TRUE),
('wf-redis-get-binding', 'Redis Get Binding', 'GET binding Secret', 'GET_BINDING', '/webhook/osb-redis-get-binding', 'osbWfRedisGetBinding', TRUE),
('wf-redis-instance-last-op', 'Redis Instance Last Op', 'GET Redis as last operation', 'INSTANCE_LAST_OPERATION', '/webhook/osb-redis-instance-last-operation', 'osbWfRedisInstanceLastOp', TRUE),
('wf-redis-binding-last-op', 'Redis Binding Last Op', 'GET binding Secret as last operation', 'BINDING_LAST_OPERATION', '/webhook/osb-redis-binding-last-operation', 'osbWfRedisBindingLastOp', TRUE),
-- Keycloak
('wf-kc-provision', 'Keycloak Provision', 'Create realm', 'PROVISION', '/webhook/osb-kc-provision', 'osbWfKcProvision', TRUE),
('wf-kc-deprovision', 'Keycloak Deprovision', 'Delete realm', 'DEPROVISION', '/webhook/osb-kc-deprovision', 'osbWfKcDeprovision', TRUE),
('wf-kc-update', 'Keycloak Update', 'Update realm', 'UPDATE', '/webhook/osb-kc-update', 'osbWfKcUpdate', TRUE),
('wf-kc-bind', 'Keycloak Bind', 'Create binding client', 'BIND', '/webhook/osb-kc-bind', 'osbWfKcBind', TRUE),
('wf-kc-unbind', 'Keycloak Unbind', 'Check/remove binding client', 'UNBIND', '/webhook/osb-kc-unbind', 'osbWfKcUnbind', TRUE),
('wf-kc-get-instance', 'Keycloak Get Instance', 'GET realm', 'GET_INSTANCE', '/webhook/osb-kc-get-instance', 'osbWfKcGetInstance', TRUE),
('wf-kc-get-binding', 'Keycloak Get Binding', 'GET binding client', 'GET_BINDING', '/webhook/osb-kc-get-binding', 'osbWfKcGetBinding', TRUE),
('wf-kc-instance-last-op', 'Keycloak Instance Last Op', 'GET realm as last operation', 'INSTANCE_LAST_OPERATION', '/webhook/osb-kc-instance-last-operation', 'osbWfKcInstanceLastOp', TRUE),
('wf-kc-binding-last-op', 'Keycloak Binding Last Op', 'GET binding client as last operation', 'BINDING_LAST_OPERATION', '/webhook/osb-kc-binding-last-operation', 'osbWfKcBindingLastOp', TRUE);

-- Client type + instance links
INSERT INTO workflow_client_types (workflow_id, client_type)
SELECT id, 'GIT' FROM workflow_definitions WHERE id LIKE 'wf-git-%' AND enabled;

INSERT INTO workflow_client_types (workflow_id, client_type)
SELECT id, 'KUBERNETES' FROM workflow_definitions WHERE id LIKE 'wf-redis-%' AND enabled;

INSERT INTO workflow_client_types (workflow_id, client_type)
SELECT id, 'HTTP' FROM workflow_definitions WHERE id LIKE 'wf-kc-%' AND enabled;

INSERT INTO workflow_git_clients (workflow_id, git_client_id)
SELECT id, 'git-demo-templates' FROM workflow_definitions WHERE id LIKE 'wf-git-%' AND enabled;

INSERT INTO workflow_kubernetes_clients (workflow_id, kubernetes_client_id)
SELECT id, 'k8s-local-dev' FROM workflow_definitions WHERE id LIKE 'wf-redis-%' AND enabled;

INSERT INTO workflow_http_clients (workflow_id, http_client_id)
SELECT id, 'http-keycloak-admin' FROM workflow_definitions WHERE id LIKE 'wf-kc-%' AND enabled;

-- Template links
INSERT INTO workflow_templates (workflow_id, template_id) VALUES
    ('wf-git-provision', 'tpl-git-instance-file'),
    ('wf-git-deprovision', 'tpl-git-instance-file'),
    ('wf-git-update', 'tpl-git-instance-file'),
    ('wf-git-get-instance', 'tpl-git-instance-file'),
    ('wf-git-instance-last-op', 'tpl-git-instance-file'),
    ('wf-git-bind', 'tpl-git-bind-file'),
    ('wf-git-unbind', 'tpl-git-bind-file'),
    ('wf-git-get-binding', 'tpl-git-bind-file'),
    ('wf-git-binding-last-op', 'tpl-git-bind-file'),
    ('wf-redis-provision', 'tpl-redis-cache'),
    ('wf-redis-deprovision', 'tpl-redis-cache'),
    ('wf-redis-update', 'tpl-redis-cache'),
    ('wf-redis-get-instance', 'tpl-redis-cache'),
    ('wf-redis-instance-last-op', 'tpl-redis-cache'),
    ('wf-redis-bind', 'tpl-redis-bind-secret'),
    ('wf-redis-unbind', 'tpl-redis-bind-secret'),
    ('wf-redis-get-binding', 'tpl-redis-bind-secret'),
    ('wf-redis-binding-last-op', 'tpl-redis-bind-secret'),
    ('wf-kc-provision', 'tpl-kc-realm-create'),
    ('wf-kc-deprovision', 'tpl-kc-realm-delete'),
    ('wf-kc-update', 'tpl-kc-realm-update'),
    ('wf-kc-get-instance', 'tpl-kc-realm-get'),
    ('wf-kc-instance-last-op', 'tpl-kc-realm-get'),
    ('wf-kc-bind', 'tpl-kc-client-create'),
    ('wf-kc-unbind', 'tpl-kc-client-delete'),
    ('wf-kc-get-binding', 'tpl-kc-client-get'),
    ('wf-kc-binding-last-op', 'tpl-kc-client-get');

-- Offering → workflow mapping
INSERT INTO offering_workflows (service_id, kind, workflow_id)
SELECT 'git-file-store', kind, id
FROM workflow_definitions
WHERE id LIKE 'wf-git-%' AND enabled;

INSERT INTO offering_workflows (service_id, kind, workflow_id)
SELECT 'redis-cache', kind, id
FROM workflow_definitions
WHERE id LIKE 'wf-redis-%' AND enabled;

INSERT INTO offering_workflows (service_id, kind, workflow_id)
SELECT 'keycloak-realm', kind, id
FROM workflow_definitions
WHERE id LIKE 'wf-kc-%' AND enabled;
