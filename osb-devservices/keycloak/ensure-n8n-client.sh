#!/usr/bin/env bash
# Ensure Keycloak client osb-n8n exists (realm import only runs on first boot).
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM="${KEYCLOAK_REALM:-osb}"
CLIENT_ID="osb-n8n"
CLIENT_SECRET="${N8N_OIDC_CLIENT_SECRET:-osb-n8n-dev-secret}"

echo "Waiting for Keycloak at ${KEYCLOAK_URL}..."
for _ in $(seq 1 60); do
  if curl -sf "${KEYCLOAK_URL}/realms/master" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

TOKEN="$(curl -sf \
  -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')"

if [[ -z "${TOKEN}" ]]; then
  echo "Failed to obtain Keycloak admin token."
  exit 1
fi

EXISTING="$(curl -sf \
  -H "Authorization: Bearer ${TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" || true)"

if echo "${EXISTING}" | grep -q "\"clientId\":\"${CLIENT_ID}\""; then
  echo "Keycloak client ${CLIENT_ID} already present."
  exit 0
fi

echo "Creating Keycloak client ${CLIENT_ID}..."
curl -sf -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
  -d "{
    \"clientId\": \"${CLIENT_ID}\",
    \"name\": \"OSB n8n Editor\",
    \"enabled\": true,
    \"protocol\": \"openid-connect\",
    \"publicClient\": false,
    \"secret\": \"${CLIENT_SECRET}\",
    \"directAccessGrantsEnabled\": false,
    \"serviceAccountsEnabled\": false,
    \"standardFlowEnabled\": true,
    \"redirectUris\": [
      \"http://localhost:5678/auth/oidc/callback\",
      \"http://127.0.0.1:5678/auth/oidc/callback\"
    ],
    \"webOrigins\": [
      \"http://localhost:5678\",
      \"http://127.0.0.1:5678\",
      \"+\"
    ]
  }"

echo "Keycloak client ${CLIENT_ID} created."
