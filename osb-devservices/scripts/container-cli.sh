#!/usr/bin/env bash
# Resolve a Compose-capable container CLI (docker or podman).
# shellcheck disable=SC2034

resolve_container_cli() {
  if [[ -n "${OSB_CONTAINER_CLI:-}" ]]; then
    if ! command -v "${OSB_CONTAINER_CLI}" >/dev/null 2>&1; then
      echo "OSB_CONTAINER_CLI=${OSB_CONTAINER_CLI} is not available in PATH." >&2
      return 1
    fi
    CONTAINER_CLI="${OSB_CONTAINER_CLI}"
    return 0
  fi

  if command -v docker >/dev/null 2>&1; then
    CONTAINER_CLI=docker
    return 0
  fi

  if command -v podman >/dev/null 2>&1; then
    CONTAINER_CLI=podman
    return 0
  fi

  echo "Neither docker nor podman is available in PATH." >&2
  echo "Install Docker or Podman, or set OSB_CONTAINER_CLI." >&2
  return 1
}

compose() {
  resolve_container_cli || return 1
  "${CONTAINER_CLI}" compose "$@"
}
