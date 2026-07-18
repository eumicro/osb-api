#!/usr/bin/env bash
# Import seed workflows into a running n8n container and publish them.
# Invoked from osb-devservices/scripts/up.sh after compose is healthy.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKFLOWS_DIR="${SCRIPT_DIR}/workflows"
CONTAINER="${N8N_CONTAINER:-osb-n8n}"

if ! command -v podman >/dev/null 2>&1 && ! command -v docker >/dev/null 2>&1; then
  echo "Neither podman nor docker found; skip n8n workflow import."
  exit 0
fi

CLI=podman
command -v podman >/dev/null 2>&1 || CLI=docker

shopt -s nullglob
host_files=("${WORKFLOWS_DIR}"/*.json)
if [[ ${#host_files[@]} -eq 0 ]]; then
  echo "No workflow JSON files in ${WORKFLOWS_DIR}; skip n8n seed."
  exit 0
fi

echo "Waiting for ${CONTAINER} health..."
ready=0
for _ in $(seq 1 60); do
  if ${CLI} exec "${CONTAINER}" wget -qO- http://127.0.0.1:5678/healthz >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done
if [[ "${ready}" -ne 1 ]]; then
  echo "n8n container ${CONTAINER} did not become healthy in time." >&2
  exit 1
fi

# Expand globs inside the container (host path /import/... does not exist on the host).
echo "Importing ${#host_files[@]} workflow(s) from /import/workflows/..."
${CLI} exec "${CONTAINER}" sh -c '
  set -eu
  count=0
  for f in /import/workflows/*.json; do
    if [ ! -f "$f" ]; then
      continue
    fi
    echo "  -> $f"
    n8n import:workflow --input="$f"
    count=$((count + 1))
  done
  if [ "$count" -eq 0 ]; then
    echo "No workflow files mounted at /import/workflows/*.json" >&2
    exit 1
  fi
  echo "Imported $count workflow(s)."
'

# Publish fixed ids used by OSB offering_workflows / seed data (scenarios × lifecycle)
publish_ids=(
  osbWfGitProvision osbWfGitDeprovision osbWfGitUpdate
  osbWfGitBind osbWfGitUnbind osbWfGitGetInstance osbWfGitGetBinding
  osbWfGitInstanceLastOp osbWfGitBindingLastOp
  osbWfRedisProvision osbWfRedisDeprovision osbWfRedisUpdate
  osbWfRedisBind osbWfRedisUnbind osbWfRedisGetInstance osbWfRedisGetBinding
  osbWfRedisInstanceLastOp osbWfRedisBindingLastOp
  osbWfKcProvision osbWfKcDeprovision osbWfKcUpdate
  osbWfKcBind osbWfKcUnbind osbWfKcGetInstance osbWfKcGetBinding
  osbWfKcInstanceLastOp osbWfKcBindingLastOp
  osbWfOsbProvision osbWfOsbDeprovision osbWfOsbUpdate
  osbWfOsbBind osbWfOsbUnbind osbWfOsbGetInstance osbWfOsbGetBinding
  osbWfOsbInstanceLastOp osbWfOsbBindingLastOp
)

failed=0
for id in "${publish_ids[@]}"; do
  echo "Publishing ${id}..."
  if ! ${CLI} exec "${CONTAINER}" n8n publish:workflow --id="${id}"; then
    echo "WARN: publish failed for ${id}" >&2
    failed=$((failed + 1))
  fi
done

wait_n8n_health() {
  for _ in $(seq 1 60); do
    if ${CLI} exec "${CONTAINER}" wget -qO- http://127.0.0.1:5678/healthz >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

# n8n CLI: "Changes will not take effect if n8n is running" — publish, then restart.
# Do NOT publish again while the process is up; that leaves webhooks without an active version.
restart_and_wait() {
  echo "Restarting ${CONTAINER} to activate webhooks..."
  ${CLI} restart "${CONTAINER}" >/dev/null
  echo "Waiting for ${CONTAINER} after restart..."
  wait_n8n_health || {
    echo "ERROR: ${CONTAINER} did not become healthy after restart." >&2
    return 1
  }
}

# Minimal OSB webhook body so the workflow actually starts (empty {} yields empty responses).
PROBE_BODY='{"operationId":"webhook-probe","kind":"DEPROVISION","kubernetesClientIds":["k8s-local-dev"],"gitClientIds":["git-demo-templates"],"httpClientIds":["http-keycloak-admin"],"command":{"instanceId":"__webhook-probe__","serviceId":"probe","planId":"probe"},"templateIds":[]}'

webhook_has_active_version() {
  local path="$1"
  local body
  body="$(${CLI} exec "${CONTAINER}" wget -qO- --post-data="${PROBE_BODY}" \
    --header='Content-Type: application/json' \
    "http://127.0.0.1:5678/webhook/${path}" 2>/dev/null || true)"
  if [[ "${body}" == *"Active version not found"* ]]; then
    return 1
  fi
  # Any non-empty response means the production webhook is registered.
  [[ -n "${body}" ]]
}

wait_webhooks_ready() {
  local paths=(
    osb-redis-provision
    osb-redis-deprovision
    osb-git-provision
    osb-kc-provision
    osb-platform-provision
    osb-platform-deprovision
  )
  local attempt
  for attempt in $(seq 1 40); do
    local missing=0
    local path
    for path in "${paths[@]}"; do
      if ! webhook_has_active_version "${path}"; then
        missing=1
        break
      fi
    done
    if [[ "${missing}" -eq 0 ]]; then
      echo "Production webhooks ready (attempt ${attempt})."
      return 0
    fi
    sleep 2
  done
  return 1
}

restart_and_wait || exit 1

echo "Waiting until production webhooks have an active version..."
if ! wait_webhooks_ready; then
  echo "WARN: webhooks still racing — restarting once more..."
  restart_and_wait || exit 1
  if ! wait_webhooks_ready; then
    echo "ERROR: webhooks still report 'Active version not found' after second restart." >&2
    failed=$((failed + 1))
  fi
fi

if [[ "${failed}" -gt 0 ]]; then
  echo "n8n seed finished with ${failed} warning(s)." >&2
  exit 1
fi

echo "n8n seed workflows import finished."
