#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$ROOT"

# shellcheck source=container-cli.sh
source "${SCRIPT_DIR}/container-cli.sh"

resolve_container_cli
echo "Using container CLI: ${CONTAINER_CLI}"

if [[ "${OSB_SKIP_KIND:-false}" != "true" ]] && [[ -f "${ROOT}/scripts/kind-down.sh" ]]; then
  echo "=== Kind: delete test cluster ==="
  bash "${ROOT}/scripts/kind-down.sh" || true
fi

compose --env-file .env -f docker-compose.yml down
