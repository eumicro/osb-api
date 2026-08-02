# OSB Helm Chart

Deploys **osb-api** and **osb-bff** (Admin UI) to Kubernetes. Images and this chart are published to GHCR.

## Install from GHCR (OCI)

```bash
helm install osb oci://ghcr.io/eumicro/osb-api/osb --version 0.1.1 \
  --namespace osb --create-namespace \
  --set config.postgres.password=osb \
  --set config.n8n.clientToken=osb-n8n-client-dev-secret \
  --set config.n8n.bridgeSecret=osb-n8n-bridge-dev-secret
```

Images and the OCI chart are **public** on GHCR — no registry login for pull.

## Local install (from repo)

```bash
helm upgrade --install osb ./charts/osb -n osb --create-namespace \
  --set imageTag=0.1.1 \
  --set config.postgres.jdbcUrl=jdbc:postgresql://postgres.osb.svc:5432/osb \
  --set config.postgres.password=osb
```

## Dependencies (not packaged)

Provide reachable services and point `config.*` at them:

| Service | Typical URL |
| --- | --- |
| Postgres | `jdbc:postgresql://postgres:5432/osb` |
| Keycloak | `config.keycloak.url` / OIDC paths |
| n8n | `config.n8n.baseUrl` |

**OpenBao** is packaged in this chart (`openbao.enabled`, default `true`). By default it uses **PostgreSQL** storage (`openbao.storage.type=postgresql`, database `openbao` on the same Postgres instance). Create that database before install. Set `openbao.devMode=true` for ephemeral in-memory `-dev` (Compose-like). Set `openbao.enabled=false` and `config.secrets.provider=memory` for an in-process SecretStore.

## Values (high level)

| Key | Description |
| --- | --- |
| `imageTag` | Tag for both images (default: `Chart.appVersion`) |
| `api.image.repository` | `ghcr.io/eumicro/osb-api/osb-api` |
| `bff.image.repository` | `ghcr.io/eumicro/osb-api/osb-bff` |
| `openbao.enabled` | Deploy OpenBao and wire API SecretStore |
| `openbao.storage.type` | `postgresql` (default) or `file` |
| `openbao.devMode` | Ephemeral `-dev` / inmem (default `false`) |
| `config.secrets.provider` | `memory` \| `openbao` (empty = follow `openbao.enabled`) |
| `ingress.enabled` | Expose BFF (and optional API paths) |
| `config.postgres.*` | JDBC + credentials |
| `config.keycloak.*` | OIDC backchannel + browser URLs |
| `*.existingSecret` | Inject extra env from a Secret |

See [`values.yaml`](values.yaml) for the full schema.
