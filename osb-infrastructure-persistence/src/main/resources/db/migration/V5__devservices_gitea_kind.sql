-- Align seed clients with local osb-devservices (Gitea + Kind).
-- Idempotent if V2 already inserted the final values.

UPDATE git_client_instances
SET name = 'Gitea Instance Files',
    description = 'Commits Test-<instanceId>.txt into the local Gitea template repo.',
    remote_url = 'http://localhost:3000/osb/git-demo-templates.git',
    default_branch = 'main',
    auth_method = 'HTTPS',
    username = 'osb',
    secret = 'osb',
    enabled = TRUE
WHERE id = 'git-demo-templates';

UPDATE kubernetes_client_instances
SET name = 'Kind Dev Cluster',
    description = 'Kind cluster API. Token is filled by kind-up.sh (ServiceAccount osb-broker).',
    api_server_url = 'https://127.0.0.1:6443',
    default_namespace = 'default',
    auth_type = 'BEARER',
    token = COALESCE(NULLIF(token, ''), 'REPLACE_VIA_KIND_UP'),
    insecure_skip_tls_verify = TRUE,
    enabled = TRUE
WHERE id = 'k8s-local-dev';
