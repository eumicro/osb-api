# OSB Helm Chart

Deploys **osb-api** and **osb-bff** (Admin UI) to Kubernetes. Images and this chart are published to GHCR.

## Install from GHCR (OCI)

```bash
helm install osb oci://ghcr.io/eumicro/osb-api/osb --version 0.1.0 \
  --namespace osb --create-namespace \
  --set config.postgres.password=osb \
  --set config.n8n.clientToken=osb-n8n-client-dev-secret \
  --set config.n8n.bridgeSecret=osb-n8n-bridge-dev-secret
```

Private packages:

```bash
echo "$GITHUB_TOKEN" | helm registry login ghcr.io -u USERNAME --password-stdin
```

## Local install (from repo)

```bash
helm upgrade --install osb ./charts/osb -n osb --create-namespace \
  --set imageTag=0.1.0 \
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

## Values (high level)

| Key | Description |
| --- | --- |
| `imageTag` | Tag for both images (default: `Chart.appVersion`) |
| `api.image.repository` | `ghcr.io/eumicro/osb-api/osb-api` |
| `bff.image.repository` | `ghcr.io/eumicro/osb-api/osb-bff` |
| `ingress.enabled` | Expose BFF (and optional API paths) |
| `config.postgres.*` | JDBC + credentials |
| `config.keycloak.*` | OIDC backchannel + browser URLs |
| `*.existingSecret` | Inject extra env from a Secret |

See [`values.yaml`](values.yaml) for the full schema.
