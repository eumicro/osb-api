# Homelab deploy: central Keycloak + OSB platform

Public access is **HTTPS only** (`*.cloudapplication.lan` via Traefik). HTTP is redirected to HTTPS. In-cluster service DNS stays HTTP.

## Hosts (Traefik / DNS → cluster LB)

| Host | Service |
|------|---------|
| `https://keycloak.cloudapplication.lan` | Keycloak (NS `keycloak`) |
| `https://emergence-platform.cloudapplication.lan` | osb-bff UI |
| `https://n8n.emergence-platform.cloudapplication.lan` | n8n |

DNS must resolve these names to the Traefik LoadBalancer IPs (same as `domain-service-dev`).

## Deploy order

```bash
# 1) Central Keycloak + Postgres + realm osb-api
kubectl apply -k deploy/homelab/keycloak

# 2) Platform Postgres + n8n
kubectl apply -k deploy/homelab/emergence-platform

# Wait until Keycloak and Postgres are ready
kubectl -n keycloak rollout status deploy/postgres deploy/keycloak --timeout=300s
kubectl -n emergence-platform rollout status deploy/postgres deploy/n8n --timeout=300s

# 3) osb-api + osb-bff + OpenBao (SecretStore)
helm upgrade --install osb oci://ghcr.io/eumicro/osb-api/osb --version 0.1.5 \
  -n emergence-platform \
  -f deploy/homelab/emergence-platform/osb-values.yaml
```

## Demo login

- UI: https://emergence-platform.cloudapplication.lan
- User: `alice` / `alice` (realm `osb-api`)
- Keycloak admin: `admin` / `admin` at https://keycloak.cloudapplication.lan

## Notes

- Realm import runs on first Keycloak start (`start-dev --import-realm`).
- Keycloak image is **19.0.3** (26.x needs x86-64-v2; Latitude D830 CPUs do not support it).
- OpenBao uses Postgres DB **`openbao`** (created in `postgres-init` / live `CREATE DATABASE`). Storage is durable; unseal key + root token live in Secret `osb-openbao-keys`.
- n8n workflows from `osb-devservices/n8n/workflows/` are **not** auto-imported yet.
- `domain-service-dev` Keycloak is unchanged.
