# Contributing

Thanks for your interest in improving OSB-API.

## Development setup

Requirements: **Java 25+**, **Node.js ≥ 20**, **Docker or Podman** (Compose), Maven Wrapper (`./mvnw`). Optional for Kubernetes realtests: `kind` + `kubectl`.

```bash
cp -n osb-devservices/.env.example osb-devservices/.env
./osb-devservices/scripts/up.sh
# UI: http://localhost:8081
```

Build and typecheck:

```bash
./mvnw test
./mvnw -DskipTests package   # optional local image/package step after tests
cd frontend && npm ci && npm run typecheck
```

CI runs `./mvnw -B test` on every PR (`CI` workflow) including:
- catalog contract smoke (`CatalogContractSmokeTest`: missing/invalid Basic → 401, valid client → offerings)
- ArchUnit dependency rules (`ArchitectureTest`: domain/application/ports stay free of API/infra/n8n)

Container image builds may still use `-DskipTests` because the verify job is required separately.

More detail: [README.md](./README.md) (Quick start / Manual start) and [osb-devservices/README.md](./osb-devservices/README.md).

## Pull requests

1. Fork the repo and create a branch from `main`.
2. Keep changes focused; follow the module layout (domain inward; adapters do not leak into `osb/`).
3. Prefer Conventional Commits (`feat`, `fix`, `docs`, `chore`, …) — releases are driven by [Release Please](./docs/RELEASING.md).
4. Run `./mvnw test` and, for UI changes, `npm run typecheck` in `frontend/` before opening a PR.
5. Open a PR against `main` and describe **why** the change is needed.

## Modules

Backend is a Maven multi-module Quarkus project (`osb-*`). The Admin UI lives under `frontend/` and is packaged into `osb-bff` via Quinoa. Local infra and realtest seeds are under `osb-devservices/`.

## Issues

Bug reports and feature ideas are welcome via [GitHub Issues](https://github.com/eumicro/osb-api/issues). Please include steps to reproduce and, when relevant, which API surface (OSB `/v2`, Admin `/api/admin`, BFF/UI) is involved.

## License

By contributing, you agree that your contributions will be licensed under the MIT License of this repository.
