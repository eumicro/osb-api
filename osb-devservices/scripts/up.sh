#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$ROOT"

# shellcheck source=container-cli.sh
source "${SCRIPT_DIR}/container-cli.sh"

if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "Created .env from .env.example"
fi

resolve_container_cli
echo "Using container CLI: ${CONTAINER_CLI}"

# Build + start infra and app images (osb-api / osb-bff with JDWP :5005/:5006).
echo "Building and starting Compose stack (images: osb-api:local, osb-bff:local)..."
compose --env-file .env -f docker-compose.yml up -d --build --wait
compose --env-file .env -f docker-compose.yml ps

# Ensure n8n OIDC client exists even if realm was imported earlier
if [[ -f "${ROOT}/keycloak/ensure-n8n-client.sh" ]]; then
  echo
  echo "=== Keycloak: ensure n8n OIDC client ==="
  bash "${ROOT}/keycloak/ensure-n8n-client.sh" || true
fi

# Seed n8n + Gitea in background — do not block stack readiness / app start
LOG_DIR="${ROOT}/.logs"
mkdir -p "${LOG_DIR}"
echo
echo "=== Seeding n8n + Gitea (background, non-blocking) ==="

if [[ -f "${ROOT}/n8n/import-workflows.sh" ]]; then
  N8N_SEED_LOG="${LOG_DIR}/n8n-seed.log"
  (
    echo "[n8n] seed workflows starting..."
    if bash "${ROOT}/n8n/import-workflows.sh"; then
      echo "[n8n] seed workflows done."
    else
      echo "WARN: [n8n] workflow seed failed — re-run n8n/import-workflows.sh" >&2
      exit 1
    fi
  ) >"${N8N_SEED_LOG}" 2>&1 &
  disown $! 2>/dev/null || true
  echo "[n8n] seed started in background (log: ${N8N_SEED_LOG})"
else
  echo "WARN: missing ${ROOT}/n8n/import-workflows.sh — n8n workflows not seeded." >&2
fi

if [[ -f "${ROOT}/scripts/gitea-seed.sh" ]]; then
  GITEA_SEED_LOG="${LOG_DIR}/gitea-seed.log"
  (
    echo "[gitea] seed repos starting..."
    if bash "${ROOT}/scripts/gitea-seed.sh"; then
      echo "[gitea] seed repos done."
    else
      echo "WARN: [gitea] seed failed — re-run scripts/gitea-seed.sh" >&2
      exit 1
    fi
  ) >"${GITEA_SEED_LOG}" 2>&1 &
  disown $! 2>/dev/null || true
  echo "[gitea] seed started in background (log: ${GITEA_SEED_LOG})"
fi

# Kind cluster for K8s provision tests (requires kind + kubectl on host)
if [[ "${OSB_SKIP_KIND:-false}" != "true" ]] && [[ -f "${ROOT}/scripts/kind-up.sh" ]]; then
  echo
  echo "=== Kind: local test cluster ==="
  if ! bash "${ROOT}/scripts/kind-up.sh"; then
    echo "WARN: Kind setup failed (install kind/kubectl, or set OSB_SKIP_KIND=true)." >&2
  fi
fi

# Apps in Compose need Keycloak/Gitea via service DNS and Kind via host-gateway.
if [[ -f "${ROOT}/scripts/compose-client-urls.sh" ]]; then
  echo
  echo "=== Compose: infrastructure client URLs ==="
  bash "${ROOT}/scripts/compose-client-urls.sh" || true
fi

echo
echo "Postgres : localhost:5432 (osb/osb)"
echo "Keycloak : http://localhost:8180  (admin/admin, realm osb)"
echo "n8n      : http://localhost:5678  (SSO via Keycloak client osb-n8n, users e.g. alice/alice)"
echo "Gitea    : http://localhost:3000  (osb/osb)  repo osb/git-demo-templates"
echo "osb-api  : http://localhost:8080  (debug attach :5005)"
echo "osb-bff  : http://localhost:8081  (debug attach :5006, UI)"
echo "Kind     : https://127.0.0.1:6443  (kubeconfig: kind/kubeconfig, skip with OSB_SKIP_KIND=true)"
echo "Seeds    : n8n + Gitea run in background (logs under .logs/)"
