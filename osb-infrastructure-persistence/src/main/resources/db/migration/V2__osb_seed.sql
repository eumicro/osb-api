-- Minimal seed for the three Realtest scenarios (offerings/workflows in V7).
-- Catalogs + the Git/K8s clients that V5/V7 point at local Devservices.

INSERT INTO catalogs (id, name, description) VALUES
    ('default', 'Default Catalog', 'Git file store + Keycloak realm offerings'),
    ('k8s-ops', 'Kubernetes Ops Catalog', 'Redis cache offering on Kind');

INSERT INTO kubernetes_client_instances (
    id, name, description, api_server_url, default_namespace, auth_type,
    username, token, oauth_client_id, oauth_client_secret, well_known_url,
    insecure_skip_tls_verify, timeout_seconds, enabled
) VALUES
    ('k8s-local-dev', 'Kind Dev Cluster',
     'Kind cluster API. Token is filled by kind-up.sh (ServiceAccount osb-broker).',
     'https://127.0.0.1:6443', 'default', 'BEARER',
     '', 'REPLACE_VIA_KIND_UP', '', '', '', TRUE, 30, TRUE);

INSERT INTO git_client_instances (
    id, name, description, remote_url, default_branch, auth_method,
    username, secret, passphrase, enabled
) VALUES
    ('git-demo-templates', 'Gitea Instance Files',
     'Commits Test-<instanceId>.txt into the local Gitea template repo.',
     'http://localhost:3000/osb/git-demo-templates.git', 'main', 'HTTPS',
     'osb', 'osb', '', TRUE);
