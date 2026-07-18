-- Remove leftover demo/platform seeds from databases that ran older V2–V7.
-- Keep only the three Realtest scenarios and their clients/templates/workflows.

-- Junction rows cascade from workflow_definitions; templates need workflows gone first.
DELETE FROM workflow_definitions
WHERE id NOT LIKE 'wf-git-%'
  AND id NOT LIKE 'wf-redis-%'
  AND id NOT LIKE 'wf-kc-%';

DELETE FROM templates
WHERE id NOT IN (
    'tpl-git-instance-file',
    'tpl-git-bind-file',
    'tpl-redis-cache',
    'tpl-redis-bind-secret',
    'tpl-kc-realm-create',
    'tpl-kc-realm-get',
    'tpl-kc-realm-update',
    'tpl-kc-realm-delete',
    'tpl-kc-client-create',
    'tpl-kc-client-get',
    'tpl-kc-client-delete'
);

DELETE FROM http_client_instances
WHERE id <> 'http-keycloak-admin';

DELETE FROM git_client_instances
WHERE id <> 'git-demo-templates';

DELETE FROM kubernetes_client_instances
WHERE id <> 'k8s-local-dev';

DELETE FROM platform_clients;

DELETE FROM service_plans
WHERE offering_id NOT IN ('git-file-store', 'redis-cache', 'keycloak-realm');

DELETE FROM service_offerings
WHERE id NOT IN ('git-file-store', 'redis-cache', 'keycloak-realm');
