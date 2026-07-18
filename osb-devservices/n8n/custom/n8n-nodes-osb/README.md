# n8n-nodes-osb

OSB-only n8n nodes. Each node has exactly two parameters:

1. **Template** — Admin → Templates (rendered server-side with webhook `command`)
2. **Client** — Admin → Git / Kubernetes / HTTP Clients (empty = first from workflow)

| Node | Type | Template kinds |
| --- | --- | --- |
| OSB Git | `CUSTOM.osbGit` | `GIT_COMMAND`, `TEXT` |
| OSB Kubernetes | `CUSTOM.osbKubernetes` | `KUBERNETES_RESOURCE`, `TEXT` → `manifest` |
| OSB HTTP | `CUSTOM.osbHttp` | `HTTP_REQUEST`, `TEXT` → method/path/body |

Kubernetes apply vs delete is inferred from webhook `kind` (`DEPROVISION` → delete).

## Locked-down editor

`osb-devservices` sets `NODES_INCLUDE` so the picker only shows:

- `n8n-nodes-base.webhook`
- `n8n-nodes-base.respondToWebhook`
- the three OSB nodes above

Community packages, AI, n8n workflow templates, and Python/Code imports are disabled.

## API

- `GET /api/internal/workflow-clients/{TYPE}/instances`
- `GET /api/internal/templates?kind=…`
- `POST /api/internal/workflow-clients/{TYPE}` with `{ action, payload: { templateId, clientId, … } }`

Env: `OSB_API_BASE_URL`, `OSB_N8N_CLIENT_TOKEN`, `OSB_CLIENT_ALLOW_LOCAL_FALLBACK`.

After node changes: recreate `osb-n8n` and re-run `n8n/import-workflows.sh`.
