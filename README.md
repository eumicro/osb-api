# OSB-API

Open Service Broker API with a management UI — modular Clean Architecture on **Quarkus** (Java 25) and **Vue 3**.

The broker exposes the [Open Service Broker API](https://github.com/openservicebrokerapi/servicebroker/blob/v2.17/spec.md) surface to platforms and an Admin API for catalogs, offerings, plans, instances, workflows, and infrastructure clients. Provisioning is orchestrated via **n8n** and adapters for **Git**, **Kubernetes**, and **HTTP**.

Architecture documentation: [`docs/arc42.md`](docs/arc42.md).

> **Status:** early development (`0.1.0-SNAPSHOT`). The OSB public surface currently includes catalog; Admin API, BFF/UI, and local realtest offerings are further along.

## Tech stack

| Layer | Technology |
| --- | --- |
| Backend | Java 25, Quarkus 3.31, Maven multi-module |
| Frontend | Vue 3, Vite, TypeScript, vue-i18n (served via Quinoa) |
| Auth | Keycloak OIDC (BFF web-app flow); platform auth via `osb-auth2` |
| Persistence | PostgreSQL + Flyway |
| Workflows | n8n (custom OSB nodes) |
| Local infra | Docker/Podman Compose, optional Kind cluster, Gitea |

## Repository layout

```text
osb/                              Domain + ports (DDD)
osb-workflow/                     Workflow ports (interfaces only)
osb-workflow-n8n/                 n8n adapter
osb-auth2/                        Platform + OIDC auth adapters
osb-infrastructure-persistence/   JPA / Flyway
osb-infrastructure-git/           Git client adapter
osb-infrastructure-kubernetes/    Kubernetes client adapter
osb-infrastructure-http-client/   HTTP client adapter
osb-api/                          OSB + Admin API (:8080)
osb-bff/                          Frontend BFF (:8081) — OIDC, proxy, Quinoa
osb-devservices/                  Compose (Postgres, Keycloak, n8n, Gitea) + Kind scripts
frontend/                         Vue admin UI
docs/                             Architecture (arc42)
```

Dependency rule: adapters depend inward on domain/workflow ports; the domain does not depend on Quarkus, JPA, or SDKs.

## Runtime topology

| Process | Port | Role |
| --- | --- | --- |
| `osb-api` | 8080 | OSB (`/v2/...`) + Admin (`/api/admin/...`) + internal workflow APIs |
| `osb-bff` | 8081 | Same-origin UI host, OIDC session, proxies `/api` and `/v2` |
| Vite (Quinoa) | 5173 | Hot reload behind the BFF |
| Keycloak | 8180 | Identity (realm `osb`) |
| Postgres | 5432 | Shared DB (`osb`; n8n uses DB `n8n`) |
| n8n | 5678 | Provision/deprovision workflows |
| Gitea | 3000 | Local Git for realtests |
| Kind API | 6443 | Optional local Kubernetes |

Open the UI only at **http://localhost:8081**. Tokens stay in the BFF session (no browser→API CORS for the app).

```text
Browser → osb-bff (:8081) → osb-api (:8080)
                ↓
         Keycloak / Postgres / n8n / Gitea / Kind
```

## Requirements

- Java 25+
- Node.js 20+ (pulled in by Quinoa / frontend)
- Bash (Linux, macOS, WSL, or Git Bash on Windows)
- Maven Wrapper (`./mvnw`)
- Docker **or** Podman (Compose)
- Optional for Kubernetes realtests: [`kind`](https://kind.sigs.k8s.io/) + [`kubectl`](https://kubernetes.io/docs/tasks/tools/)

## Quick start (Cursor / VS Code)

1. Install the recommended extensions (Java Pack, Quarkus, Vue - Volar, Docker).
2. Copy env defaults once:

   ```bash
   cp -n osb-devservices/.env.example osb-devservices/.env
   ```

3. **Run and Debug** → pick a compound launch:

   | Launch | What it starts |
   | --- | --- |
   | **OSB Full Stack** | Compose build/start (infra + `osb-api`/`osb-bff` images) + debug attach + Chrome |
   | **OSB App Stack (Compose apps only)** | Rebuild/restart only API/BFF containers + attach |
   | **OSB Local Dev (quarkus:dev on host)** | Classic host `quarkus:dev` (stop Compose apps first) |

4. Stop with **Debug Stop** / Shift+F5 (runs `osb:stop-app-stack` → stops Compose app containers / frees ports).

   - Task **`osb:stop-app-stack`** — Compose `osb-api`/`osb-bff` + ports  
   - Task **`osb:stop-all`** — apps + full DevServices (`compose down`)

Debug ports published by Compose: **5005** (api), **5006** (bff).

### Demo login (Keycloak realm `osb`)

| User | Password | Group |
| --- | --- | --- |
| `alice` | `alice` | osb-operators |
| `operator` | `operator` | osb-operators |
| `viewer` | `viewer` | osb-viewers |

If Keycloak was started before the `osb-bff` client existed, recreate DevServices:

```bash
./osb-devservices/scripts/down.sh
./osb-devservices/scripts/up.sh
```

## Manual start

```bash
# Preferred: build images + start everything (infra + api + bff)
cp -n osb-devservices/.env.example osb-devservices/.env
./osb-devservices/scripts/up.sh
# UI: http://localhost:8081  — debug: localhost:5005 / :5006

# Alternative: host quarkus:dev (stop Compose apps first if ports clash)
./scripts/dev-stop-app.sh
# then infra-only is not split yet — use quarkus:dev against a running Compose infra,
# or keep using Local Dev launch after stopping app containers.

# Legacy host API (ports must be free):
./mvnw -pl osb-api -am quarkus:dev

# 3) BFF + UI (Quinoa)
./mvnw -pl osb-bff -am quarkus:dev
# without login:
./mvnw -pl osb-bff -am quarkus:dev -Dquarkus.profile=no-auth
```

UI: http://localhost:8081

Maven helpers for DevServices:

```bash
./mvnw -pl osb-devservices exec:exec@up
./mvnw -pl osb-devservices exec:exec@ps
./mvnw -pl osb-devservices exec:exec@down
```

Details, Kind/Podman notes, and seeded realtest offerings: [`osb-devservices/README.md`](osb-devservices/README.md).

## Admin API (overview)

Served by `osb-api` on port 8080 (proxied through the BFF when using the UI):

| Area | Base path |
| --- | --- |
| Catalogs / offerings / plans | `/api/admin/catalogs`, `/api/admin/offerings`, `/api/admin/plans` |
| Service instances | `/api/admin/service-instances` |
| Platform clients | `/api/admin/platform-clients` |
| Workflows & templates | `/api/admin/workflows`, `/api/admin/templates` |
| Infra clients | `/api/admin/git-clients`, `/api/admin/kubernetes-clients`, `/api/admin/http-clients` |
| OSB catalog | `GET /v2/catalog` |

Internal APIs used by n8n custom nodes live under `/api/internal/...`.

## Frontend

Vue 3 admin UI under `frontend/`, served by `osb-bff` via Quinoa:

- Atomic Design (`atoms` → `molecules` → `organisms` → panels/pages)
- MVC-style `models`, `services`, `controllers`, views
- Dockable workspace panels (catalogs, offerings, plans, instances, clients, workflows, templates)
- Plugin registry (`src/plugins`)
- Locales: `de`, `en`, `fr`, `pl`, `es`, `ru`

## Local realtest offerings

Flyway seeds (see `osb-devservices` README) include demo offerings that exercise the adapters:

| Offering | Backend | Provision result |
| --- | --- | --- |
| `git-file-store` | Gitea | Commits a test file into `osb/git-demo-templates` |
| `redis-cache` | Kind | Redis + Redis Commander + Ingress |
| `keycloak-realm` | Keycloak Admin HTTP | Creates realm `osb-<instanceId>` |
| `osb-platform` | Kind | Nested Postgres + `osb-api` + `osb-bff` (GHCR images) + Ingress |

## Build

```bash
./mvnw -DskipTests package
# frontend is packaged into osb-bff via Quinoa
```

For UI typecheck only:

```bash
cd frontend && npm ci && npm run typecheck
```

## Releases & container images

Release process (Conventional Commits → changelog → GitHub Release → GHCR): see [`docs/RELEASING.md`](docs/RELEASING.md).

| Workflow | Role |
| --- | --- |
| [Release Please](.github/workflows/release-please.yml) | Release PR, `CHANGELOG.md`, GitHub Release notes, then image + Helm publish |
| [Manual Release](.github/workflows/release-manual.yml) | Escape hatch: version input → tag + notes + images + chart |
| [Container Images](.github/workflows/container-images.yml) | JVM images to GHCR (`main` → `latest`; release tags → SemVer) |
| [Helm Chart](.github/workflows/helm.yml) | Lint/template; on release push OCI chart to GHCR |

Published artifacts:

```text
ghcr.io/eumicro/osb-api/osb-api:<tag>
ghcr.io/eumicro/osb-api/osb-bff:<tag>
oci://ghcr.io/eumicro/osb-api/osb:<chart-version>
```

Helm (Kubernetes dogfood):

```bash
helm install osb oci://ghcr.io/eumicro/osb-api/osb --version 0.1.0 \
  -n osb --create-namespace \
  --set config.postgres.password=osb
# see charts/osb/README.md and values-kind-example.yaml
```

Local JVM image build:

```bash
./mvnw -pl osb-api -am package -DskipTests
docker build -f osb-api/src/main/docker/Dockerfile.jvm -t osb-api ./osb-api

./mvnw -pl osb-bff -am package -DskipTests
docker build -f osb-bff/src/main/docker/Dockerfile.jvm -t osb-bff ./osb-bff
```

Pull / Compose dogfood (no local Maven image build):

```bash
docker pull ghcr.io/eumicro/osb-api/osb-api:latest
docker pull ghcr.io/eumicro/osb-api/osb-bff:latest

cd osb-devservices
docker compose --env-file .env -f docker-compose.yml -f docker-compose.ghcr.yml pull
docker compose --env-file .env -f docker-compose.yml -f docker-compose.ghcr.yml up -d --no-build --wait
```

If packages are private, `docker login ghcr.io` with a PAT that has `read:packages`. After the first publish you can set package visibility to public under GitHub → Packages.

## Documentation

| Doc | Description |
| --- | --- |
| [`docs/arc42.md`](docs/arc42.md) | Architecture (goals, context, building blocks, runtime, ADRs) |
| [`docs/RELEASING.md`](docs/RELEASING.md) | Release Please, notes, SemVer, GHCR |
| [`CHANGELOG.md`](CHANGELOG.md) | Generated release history |
| [`charts/osb/README.md`](charts/osb/README.md) | Helm chart (OCI on GHCR) |
| [`osb-devservices/README.md`](osb-devservices/README.md) | Local infrastructure, Kind, Gitea, realtest seeds |

## License

License not yet declared in this repository.
