#!/usr/bin/env bash
# Delete the local Kind cluster "osb".
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=container-cli.sh
source "${SCRIPT_DIR}/container-cli.sh"

CLUSTER_NAME="${KIND_CLUSTER_NAME:-osb}"
KUBECONFIG_OUT="${ROOT}/kind/kubeconfig"

if ! command -v kind >/dev/null 2>&1; then
  echo "kind not found — nothing to delete."
  exit 0
fi

resolve_container_cli || true
export KIND_EXPERIMENTAL_PROVIDER="${KIND_EXPERIMENTAL_PROVIDER:-${CONTAINER_CLI:-podman}}"

if kind get clusters 2>/dev/null | grep -qx "${CLUSTER_NAME}"; then
  echo "Deleting Kind cluster '${CLUSTER_NAME}'..."
  kind delete cluster --name "${CLUSTER_NAME}"
else
  echo "Kind cluster '${CLUSTER_NAME}' not found."
fi

rm -f "${KUBECONFIG_OUT}"
