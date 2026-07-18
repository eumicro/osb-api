#!/usr/bin/env bash
# Point infrastructure clients at Compose DNS / host-gateway when apps run in containers.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=container-cli.sh
source "${SCRIPT_DIR}/container-cli.sh"
resolve_container_cli

CONTAINER="${OSB_POSTGRES_CONTAINER:-osb-postgres}"
USER="${POSTGRES_USER:-osb}"
DB="${POSTGRES_DB:-osb}"

if ! ${CONTAINER_CLI} exec "${CONTAINER}" pg_isready -U "${USER}" -d "${DB}" >/dev/null 2>&1; then
  echo "WARN: ${CONTAINER} not ready — skip compose client URL patch." >&2
  exit 0
fi

echo "Patching infrastructure client URLs for Compose app containers..."
# -i is required so the SQL heredoc reaches psql inside the container.
${CONTAINER_CLI} exec -i "${CONTAINER}" psql -U "${USER}" -d "${DB}" -v ON_ERROR_STOP=1 <<'SQL'
UPDATE http_client_instances
SET base_url = 'http://keycloak:8080',
    well_known_url = 'http://keycloak:8080/realms/master/.well-known/openid-configuration'
WHERE id = 'http-keycloak-admin';

UPDATE git_client_instances
SET remote_url = 'http://gitea:3000/osb/git-demo-templates.git'
WHERE id = 'git-demo-templates';

UPDATE kubernetes_client_instances
SET api_server_url = 'https://host.docker.internal:6443'
WHERE id = 'k8s-local-dev';
SQL

echo "Compose client URLs updated (keycloak/gitea/kind via host.docker.internal)."
${CONTAINER_CLI} exec "${CONTAINER}" psql -U "${USER}" -d "${DB}" -t -A -c \
  "SELECT id || '=' || api_server_url FROM kubernetes_client_instances WHERE id='k8s-local-dev';"
