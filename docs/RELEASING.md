# Releasing

This repository uses **[Release Please](https://github.com/googleapis/release-please)** for day-to-day releases
and **GitHub Releases** + **GHCR** for artifacts. Commit messages should follow
[Conventional Commits](https://www.conventionalcommits.org/).

## What a release includes

| Artifact | Location |
| --- | --- |
| Git tag | `vX.Y.Z` |
| GitHub Release + notes | [Releases](https://github.com/eumicro/osb-api/releases) |
| Changelog | [`CHANGELOG.md`](../CHANGELOG.md) |
| Maven version | all `pom.xml` (then next `*-SNAPSHOT`) |
| UI version | `frontend/package.json` |
| Container images | `ghcr.io/eumicro/osb-api/osb-api:X.Y.Z` and `.../osb-bff:X.Y.Z` (+ `latest` from `main`) |
| Helm chart (OCI) | `oci://ghcr.io/eumicro/osb-api/osb` version `X.Y.Z` |

## Conventional Commits → SemVer

| Prefix | Effect (pre-1.0: minor/patch bumps are enabled) |
| --- | --- |
| `feat:` | minor |
| `fix:` | patch |
| `feat!:` / `BREAKING CHANGE:` | major (or minor while `< 1.0.0`) |
| `docs:`, `chore:`, `refactor:`, … | listed in notes; may not bump alone |

Examples:

```text
feat: add redis dashboard URL from workflow
fix: activate n8n webhooks after import
chore: bump quarkus platform
```

## Normal flow (Release Please)

1. Merge work to `main` with conventional commits.
2. Workflow **Release Please** opens (or updates) a **release PR** with:
   - version bumps in all `pom.xml` + `frontend/package.json`
   - `CHANGELOG.md` section for the release
   - `.release-please-manifest.json` update
3. Review and merge the release PR.
4. Release Please creates the GitHub Release (`vX.Y.Z`) and release notes.
5. Jobs **Publish release images** and **Publish Helm chart** push GHCR images and the
   OCI Helm chart for that tag (`workflow_call` — `GITHUB_TOKEN` tag events do not chain).
6. A follow-up **SNAPSHOT** PR may appear (`X.Y.Z+1-SNAPSHOT`) for Maven; merge it.

Trigger manually anytime: Actions → **Release Please** → Run workflow.

### Optional: `RELEASE_PLEASE_TOKEN`

Repo secret with a classic PAT (`repo`, `workflow`) or fine-grained token that can
push and open PRs. Benefits:

- CI runs on the release PR
- tag `push` events fire for other workflows

Without it, `GITHUB_TOKEN` is used; image publish still runs via `workflow_call`.

Also enable: **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests**.

## Manual release (escape hatch)

Actions → **Manual Release** → Run workflow:

| Input | Meaning |
| --- | --- |
| `version` | SemVer without `v` (e.g. `0.1.0`) |
| `prerelease` | Mark GitHub Release as prerelease |
| `skip_snapshot_bump` | Do not commit next `*-SNAPSHOT` |

This bumps versions, updates `CHANGELOG.md`, tags `vX.Y.Z`, creates a GitHub Release
with generated notes (+ image URLs), publishes GHCR images, then bumps SNAPSHOT
(unless skipped). Prefer Release Please when possible so notes stay consistent.

## Pull published images / Helm chart

```bash
docker pull ghcr.io/eumicro/osb-api/osb-api:0.1.1
docker pull ghcr.io/eumicro/osb-api/osb-bff:0.1.1

helm install osb oci://ghcr.io/eumicro/osb-api/osb --version 0.1.1 \
  -n osb --create-namespace \
  -f charts/osb/values-kind-example.yaml

cd osb-devservices
OSB_API_IMAGE=ghcr.io/eumicro/osb-api/osb-api:0.1.1 \
OSB_BFF_IMAGE=ghcr.io/eumicro/osb-api/osb-bff:0.1.1 \
  docker compose --env-file .env -f docker-compose.yml -f docker-compose.ghcr.yml up -d --no-build --wait
```

Chart source: [`charts/osb`](../charts/osb). Details: [`charts/osb/README.md`](../charts/osb/README.md).

## First release checklist

1. Merge release automation to `main`.
2. Ensure Actions can open PRs (setting above).
3. Wait for / dispatch **Release Please** → merge the release PR (first version defaults toward `0.1.0` via `initial-version`).
4. Confirm GitHub Release notes and GHCR packages.
5. Confirm GHCR packages stay **public** (`osb-api`, `osb-bff`, Helm `osb`) under GitHub → Packages (anonymous pull).

## Branch & tag protection

Repository rulesets (Settings → Rules → Rulesets):

| Ruleset | Applies to | Enforces |
| --- | --- | --- |
| **Protect main** | default branch | PR required, CI `osb-api` + `osb-bff` must pass (branch up to date), no force-push, no delete; open review threads must be resolved. Admins may bypass only when merging a PR. |
| **Protect release tags** | `v*` | no delete / retarget / force-update (admins may bypass) |

Direct pushes to `main` are blocked; changes go through pull requests.
