#!/usr/bin/env bash
# Ensure Gitea admin user + Realtest repos under gitea/repos/ (git-demo-templates).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=container-cli.sh
source "${SCRIPT_DIR}/container-cli.sh"

resolve_container_cli

GITEA_URL="${GITEA_URL:-http://localhost:${GITEA_HTTP_PORT:-3000}}"
GITEA_CONTAINER="${GITEA_CONTAINER:-osb-gitea}"
USER_NAME="${GITEA_ADMIN_USER:-osb}"
USER_PASS="${GITEA_ADMIN_PASSWORD:-osb}"
USER_EMAIL="${GITEA_ADMIN_EMAIL:-osb@localhost}"
REPOS_DIR="${ROOT}/gitea/repos"

wait_gitea() {
  local i
  for i in $(seq 1 60); do
    if curl -sf "${GITEA_URL}/api/healthz" >/dev/null 2>&1 \
      || curl -sf "${GITEA_URL}/" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "ERROR: Gitea not healthy at ${GITEA_URL}" >&2
  return 1
}

ensure_repo() {
  local repo_name="$1"
  local description="$2"
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' "${AUTH[@]}" \
    "${GITEA_URL}/api/v1/repos/${USER_NAME}/${repo_name}")"
  if [[ "${code}" == "404" ]]; then
    echo "Creating repo ${USER_NAME}/${repo_name}..."
    curl -sf "${AUTH[@]}" -X POST \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"${repo_name}\",\"description\":\"${description}\",\"private\":false,\"auto_init\":true,\"default_branch\":\"main\"}" \
      "${GITEA_URL}/api/v1/user/repos" >/dev/null
  else
    echo "Repo ${USER_NAME}/${repo_name} already exists (HTTP ${code})."
  fi
}

# Upsert a file into a repo via Contents API
upsert_file() {
  local repo_name="$1"
  local rel="$2"
  local abs="$3"
  local api_path="${GITEA_URL}/api/v1/repos/${USER_NAME}/${repo_name}/contents/${rel}"
  local content_b64
  content_b64="$(base64 -w0 <"${abs}" 2>/dev/null || base64 <"${abs}" | tr -d '\n')"

  local sha=""
  local existing
  existing="$(curl -s "${AUTH[@]}" "${api_path}" || true)"
  if echo "${existing}" | grep -q '"sha"'; then
    sha="$(echo "${existing}" | sed -n 's/.*"sha"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n1)"
  fi

  local body method
  if [[ -n "${sha}" ]]; then
    method=PUT
    body="$(printf '{"message":"Update %s","content":"%s","sha":"%s","branch":"main"}' \
      "${rel}" "${content_b64}" "${sha}")"
  else
    method=POST
    body="$(printf '{"message":"Add %s","content":"%s","branch":"main"}' \
      "${rel}" "${content_b64}")"
  fi

  curl -sf "${AUTH[@]}" -X "${method}" \
    -H "Content-Type: application/json" \
    -d "${body}" \
    "${api_path}" >/dev/null
  echo "  ${repo_name}: ${rel}"
}

seed_repo_dir() {
  local repo_dir="$1"
  local repo_name
  repo_name="$(basename "${repo_dir}")"
  ensure_repo "${repo_name}" "OSB template repo for client ${repo_name}"

  # Find files relative to repo dir (portable: no mapfile)
  local abs rel
  while IFS= read -r -d '' abs; do
    rel="${abs#"${repo_dir}/"}"
    # normalize Windows-style separators if any
    rel="${rel//\\//}"
    upsert_file "${repo_name}" "${rel}" "${abs}"
  done < <(find "${repo_dir}" -type f -print0)
}

if [[ ! -d "${REPOS_DIR}" ]]; then
  echo "ERROR: missing template repos dir: ${REPOS_DIR}" >&2
  exit 1
fi

if ! command -v base64 >/dev/null 2>&1; then
  echo "ERROR: base64 required for Gitea seed." >&2
  exit 1
fi
if ! command -v find >/dev/null 2>&1; then
  echo "ERROR: find required for Gitea seed." >&2
  exit 1
fi

echo "Waiting for Gitea at ${GITEA_URL}..."
wait_gitea

echo "Ensuring Gitea admin user '${USER_NAME}'..."
"${CONTAINER_CLI}" exec --user git "${GITEA_CONTAINER}" \
  gitea admin user create \
    --username "${USER_NAME}" \
    --password "${USER_PASS}" \
    --email "${USER_EMAIL}" \
    --admin \
    --must-change-password=false \
  2>/dev/null || true

AUTH=(-u "${USER_NAME}:${USER_PASS}")

echo "Seeding template repos from ${REPOS_DIR}..."
for repo_dir in "${REPOS_DIR}"/*/; do
  [[ -d "${repo_dir}" ]] || continue
  seed_repo_dir "${repo_dir%/}"
done

echo
echo "Gitea UI : ${GITEA_URL}  (${USER_NAME}/${USER_PASS})"
echo "Repos    : one per seed client under ${USER_NAME}/"
for repo_dir in "${REPOS_DIR}"/*/; do
  [[ -d "${repo_dir}" ]] || continue
  name="$(basename "${repo_dir}")"
  echo "  - ${GITEA_URL}/${USER_NAME}/${name}.git"
done
