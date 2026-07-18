# osb-devservices

Local development stack: **Postgres**, **Keycloak**, **n8n**, **Gitea**, **osb-api**, **osb-bff** (Compose-built images), plus a **Kind** test cluster (host CLI).

## Requirements

- Docker **or** Podman (Compose plugin/compatible)
- Bash
- Free ports: `5432`, `8180`, `5678`, `3000`, `8080`, `8081`, `5005`, `5006`, `6443`
- For Kind (optional but needed for K8s Realtests): [`kind`](https://kind.sigs.k8s.io/) + [`kubectl`](https://kubernetes.io/docs/tasks/tools/)
  - With Podman: Kind uses `KIND_EXPERIMENTAL_PROVIDER=podman`
  - **Windows/WSL:** Kind requires a **rootful** Podman machine (`podman machine set --rootful`). Rootless fails with `ip6tables` / `Operation not permitted`.

The scripts auto-select `docker` if present, otherwise `podman`. Override with `OSB_CONTAINER_CLI=podman` (or `docker`).

## Start / Stop

```bash
# from repo root
cp -n osb-devservices/.env.example osb-devservices/.env

./osb-devservices/scripts/up.sh
./osb-devservices/scripts/down.sh
```

Skip Kind (Compose + Gitea seed only):

```bash
OSB_SKIP_KIND=true ./osb-devservices/scripts/up.sh
```

`up.sh` runs `compose up -d --build --wait` and tags images `osb-api:local` / `osb-bff:local` (JDWP on `:5005` / `:5006`). Dockerfiles: `docker/Dockerfile.osb-*`.

### Pull GHCR images (dogfood, no local Maven build)

After [Container Images](../.github/workflows/container-images.yml) has published to GHCR:

```bash
cd osb-devservices
docker compose --env-file .env -f docker-compose.yml -f docker-compose.ghcr.yml pull
docker compose --env-file .env -f docker-compose.yml -f docker-compose.ghcr.yml up -d --no-build --wait
```

Defaults: `ghcr.io/eumicro/osb-api/osb-api:latest` and `.../osb-bff:latest` (override with `OSB_API_IMAGE` / `OSB_BFF_IMAGE`).

Or with Docker Compose directly:

```bash
cd osb-devservices
docker compose --env-file .env -f docker-compose.yml up -d --build --wait
docker compose --env-file .env -f docker-compose.yml ps
docker compose --env-file .env -f docker-compose.yml down
```

Or via Maven:

```bash
./mvnw -pl osb-devservices exec:exec@up
./mvnw -pl osb-devservices exec:exec@ps
./mvnw -pl osb-devservices exec:exec@down
```

In Cursor/VS Code: task **`devservices:up`** or compound launch **`OSB Full Stack`**.

## Endpoints

| Service | URL / Connection | Defaults |
| --- | --- | --- |
| Postgres | `localhost:5432` / DB `osb` | User/Pass: `osb` / `osb` |
| Keycloak | http://localhost:8180 | Admin: `admin` / `admin` |
| Keycloak Realm | http://localhost:8180/realms/osb | Users: `operator`/`operator`, `viewer`/`viewer` |
| n8n | http://localhost:5678 | Keycloak SSO (`osb-n8n`), embedded in OSB Admin → Workflow detail. **Node picker locked** to Webhook, Respond to Webhook, OSB Git/Kubernetes/HTTP (see `NODES_INCLUDE` in `docker-compose.yml`). Seed workflows in `n8n/workflows/`. Recreate after OIDC/node changes: `podman compose up -d --force-recreate n8n`. |
| Gitea | http://localhost:3000 | User/Pass: `osb` / `osb`. Realtest repo `osb/git-demo-templates` (from `gitea/repos/git-demo-templates`). HTTPS clone only (SSH disabled for rootless Podman). |
| Kind | https://127.0.0.1:6443 | Cluster name `osb`, namespace `osb-demo`. Kubeconfig: `kind/kubeconfig`. Created by `scripts/kind-up.sh` (not Compose). |

Additional DBs (init script): `keycloak` (unused by Keycloak start-dev), `n8n` (wired to the n8n service via `DB_TYPE=postgresdb`).

## Kind + Gitea helpers

```bash
./osb-devservices/scripts/gitea-seed.sh   # admin + git-demo-templates repo
./osb-devservices/scripts/kind-up.sh      # create/reuse cluster
./osb-devservices/scripts/kind-down.sh    # delete cluster

export KUBECONFIG="$PWD/osb-devservices/kind/kubeconfig"
kubectl get nodes
kubectl get pods -n osb-demo
```

### Kind on Podman (Windows)

If `kind-up.sh` fails with netavark/`ip6tables`, switch the machine to rootful once:

```bash
podman machine stop
podman machine set --rootful
podman machine start
./osb-devservices/scripts/kind-up.sh
```

Compose services (Postgres, Keycloak, n8n, Gitea) keep working under rootless or rootful.

Realtest offerings (Flyway `V7`/`V8`) and clients:

| Offering | Client | What provision does | Dashboard URL |
| --- | --- | --- | --- |
| `git-file-store` | `git-demo-templates` (Gitea) | Commit `Test-<instanceId>.txt` | Gitea file in `osb/git-demo-templates` |
| `redis-cache` | `k8s-local-dev` (Kind) | Apply Redis + Redis Commander + Ingress in namespace `<instanceId>` | `http://redis-ui-<instanceId>.localhost:8088/` (Kind ingress-nginx; host 8088→80) |
| `keycloak-realm` | `http-keycloak-admin` | Create realm `osb-<instanceId>` with realm-admin user (`adminUsername` / `adminPassword` / `adminPasswordTemporary`) | `http://localhost:8180/admin/osb-<instanceId>/console/` — login with provisioned credentials; temporary password forces change on first login |

`kind-up.sh` creates SA `osb-broker` and writes its Bearer token into `kubernetes_client_instances.k8s-local-dev` (and `kind/osb-broker.token`).
OSB does not assume a local kubeconfig — only the configured API URL + auth.

After changing Flyway seeds (V2–V8), recreate the Postgres volume so migrations re-apply cleanly (from repo root):

```bash
cd osb-devservices && docker compose --env-file .env down -v && cd .. && ./osb-devservices/scripts/up.sh
```
