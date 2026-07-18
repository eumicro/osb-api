#!/usr/bin/env bash
# Stop local OSB app processes: Compose osb-api/osb-bff if present, else host ports.
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEVSERVICES="${ROOT}/osb-devservices"

if [[ -f "${DEVSERVICES}/scripts/container-cli.sh" ]]; then
  # shellcheck source=/dev/null
  source "${DEVSERVICES}/scripts/container-cli.sh"
  if resolve_container_cli 2>/dev/null; then
    if [[ -f "${DEVSERVICES}/docker-compose.yml" ]]; then
      (
        cd "${DEVSERVICES}"
        if [[ -f .env ]]; then
          compose --env-file .env -f docker-compose.yml stop osb-api osb-bff >/dev/null 2>&1 || true
        else
          compose -f docker-compose.yml stop osb-api osb-bff >/dev/null 2>&1 || true
        fi
      )
      echo "Stopped Compose services osb-api / osb-bff (if they were running)."
    fi
  fi
fi

kill_port() {
  local port="$1"

  if command -v npx >/dev/null 2>&1; then
    npx --yes kill-port "$port" >/dev/null 2>&1 || true
    return
  fi

  if command -v taskkill.exe >/dev/null 2>&1; then
    local pids
    pids=$(
      netstat -ano 2>/dev/null \
        | tr -d '\r' \
        | awk -v p=":${port}" '$0 ~ p && $0 ~ /LISTENING/ { print $NF }' \
        | sort -u
    )
    for pid in $pids; do
      if [[ "$pid" =~ ^[0-9]+$ ]] && [[ "$pid" != "0" ]]; then
        taskkill.exe //F //PID "$pid" >/dev/null 2>&1 || true
      fi
    done
    return
  fi

  if command -v fuser >/dev/null 2>&1; then
    fuser -k "${port}/tcp" >/dev/null 2>&1 || true
    return
  fi

  if command -v lsof >/dev/null 2>&1; then
    local pids
    pids=$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
    if [[ -n "${pids:-}" ]]; then
      # shellcheck disable=SC2086
      kill -9 $pids >/dev/null 2>&1 || true
    fi
  fi
}

ports=(8080 8081 5005 5006 5173)
for port in "${ports[@]}"; do
  kill_port "$port"
done

echo "Stopped OSB app stack (ports: ${ports[*]})."
