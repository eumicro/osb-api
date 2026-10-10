#!/usr/bin/env bash
# Import seed workflows from osb-devservices/n8n/workflows into Homelab n8n (Postgres-backed).
# Scales n8n to 0 while a one-shot Job runs CLI import/publish, then scales back and waits for /healthz.
#
# Usage (from repo root or this directory):
#   bash deploy/homelab/emergence-platform/scripts/seed-n8n-workflows.sh
#
# Env overrides:
#   NS                 Kubernetes namespace (default: emergence-platform)
#   N8N_DEPLOY         Deployment name (default: n8n)
#   N8N_IMAGE          Image for the seed Job (default: from deploy/n8n)
#   WORKFLOWS_DIR      Host path to workflow JSON files
#   SKIP_SCALE         If 1, do not scale n8n (not recommended; CLI may race the live process)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_ROOT="$(cd "${PLATFORM_DIR}/../../.." && pwd)"
NS="${NS:-emergence-platform}"
N8N_DEPLOY="${N8N_DEPLOY:-n8n}"
WORKFLOWS_DIR="${WORKFLOWS_DIR:-${REPO_ROOT}/osb-devservices/n8n/workflows}"
CM_NAME="n8n-seed-workflows"
JOB_NAME="n8n-seed-workflows"
SKIP_SCALE="${SKIP_SCALE:-0}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl required" >&2
  exit 1
fi

shopt -s nullglob
files=("${WORKFLOWS_DIR}"/*.json)
if [[ ${#files[@]} -eq 0 ]]; then
  echo "No workflow JSON in ${WORKFLOWS_DIR}" >&2
  exit 1
fi

N8N_IMAGE="${N8N_IMAGE:-$(kubectl -n "${NS}" get deploy "${N8N_DEPLOY}" -o jsonpath='{.spec.template.spec.containers[0].image}')}"
if [[ -z "${N8N_IMAGE}" ]]; then
  N8N_IMAGE="n8nio/n8n:2.23.3"
fi

echo "=== n8n Homelab seed (${#files[@]} workflows) ==="
echo "namespace=${NS} image=${N8N_IMAGE}"
echo "workflows=${WORKFLOWS_DIR}"

echo "--- ConfigMap ${CM_NAME} ---"
kubectl -n "${NS}" create configmap "${CM_NAME}" \
  --from-file="${WORKFLOWS_DIR}" \
  --dry-run=client -o yaml | kubectl apply -f -

restore_n8n() {
  if [[ "${SKIP_SCALE}" != "1" ]]; then
    echo "--- Scale ${N8N_DEPLOY} → 1 (restore) ---"
    kubectl -n "${NS}" scale deploy/"${N8N_DEPLOY}" --replicas=1 || true
    kubectl -n "${NS}" rollout status deploy/"${N8N_DEPLOY}" --timeout=300s || true
  fi
}
trap restore_n8n EXIT

if [[ "${SKIP_SCALE}" != "1" ]]; then
  echo "--- Scale ${N8N_DEPLOY} → 0 (avoid CLI vs live process race) ---"
  kubectl -n "${NS}" scale deploy/"${N8N_DEPLOY}" --replicas=0
  kubectl -n "${NS}" wait --for=delete pod -l app=n8n --timeout=180s 2>/dev/null || true
fi

echo "--- Job ${JOB_NAME} ---"
kubectl -n "${NS}" delete job "${JOB_NAME}" --ignore-not-found=true
cat <<EOF | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
  namespace: ${NS}
  labels:
    app: n8n-seed
spec:
  ttlSecondsAfterFinished: 600
  backoffLimit: 1
  template:
    metadata:
      labels:
        app: n8n-seed
    spec:
      restartPolicy: Never
      # Prevent K8s from injecting N8N_PORT=tcp://... service links (breaks n8n CLI parsing).
      enableServiceLinks: false
      containers:
        - name: seed
          image: ${N8N_IMAGE}
          imagePullPolicy: IfNotPresent
          command: ["/bin/sh", "-c"]
          args:
            - |
              set -eu
              echo "Importing workflows from /import/workflows (directory)..."
              n8n import:workflow --separate --input=/import/workflows
              echo "Publishing fixed ids..."
              failed=0
              for id in \
                osbWfGitProvision osbWfGitDeprovision osbWfGitUpdate \
                osbWfGitBind osbWfGitUnbind osbWfGitGetInstance osbWfGitGetBinding \
                osbWfGitInstanceLastOp osbWfGitBindingLastOp \
                osbWfRedisProvision osbWfRedisDeprovision osbWfRedisUpdate \
                osbWfRedisBind osbWfRedisUnbind osbWfRedisGetInstance osbWfRedisGetBinding \
                osbWfRedisInstanceLastOp osbWfRedisBindingLastOp \
                osbWfKcProvision osbWfKcDeprovision osbWfKcUpdate \
                osbWfKcBind osbWfKcUnbind osbWfKcGetInstance osbWfKcGetBinding \
                osbWfKcInstanceLastOp osbWfKcBindingLastOp \
                osbWfOsbProvision osbWfOsbDeprovision osbWfOsbUpdate \
                osbWfOsbBind osbWfOsbUnbind osbWfOsbGetInstance osbWfOsbGetBinding \
                osbWfOsbInstanceLastOp osbWfOsbBindingLastOp
              do
                echo "Publishing \$id..."
                if ! n8n publish:workflow --id="\$id"; then
                  echo "WARN: publish failed for \$id" >&2
                  failed=\$((failed + 1))
                fi
              done
              if [ "\$failed" -gt 0 ]; then
                echo "Seed finished with \$failed publish warning(s)." >&2
                exit 1
              fi
              echo "n8n Homelab seed job finished."
          env:
            # Override K8s service-link injection (N8N_PORT=tcp://...) which breaks n8n CLI.
            - name: N8N_PORT
              value: "5678"
            - name: N8N_LISTEN_ADDRESS
              value: "127.0.0.1"
            - name: N8N_ENCRYPTION_KEY
              valueFrom:
                secretKeyRef:
                  name: platform-secrets
                  key: N8N_ENCRYPTION_KEY
            - name: DB_TYPE
              value: postgresdb
            - name: DB_POSTGRESDB_HOST
              value: postgres.emergence-platform.svc.cluster.local
            - name: DB_POSTGRESDB_PORT
              value: "5432"
            - name: DB_POSTGRESDB_DATABASE
              value: n8n
            - name: DB_POSTGRESDB_USER
              value: osb
            - name: DB_POSTGRESDB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: platform-secrets
                  key: POSTGRES_PASSWORD
            - name: N8N_DIAGNOSTICS_ENABLED
              value: "false"
            - name: N8N_PERSONALIZATION_ENABLED
              value: "false"
            - name: N8N_ONBOARDING_FLOW_DISABLED
              value: "true"
            - name: N8N_VERSION_NOTIFICATIONS_ENABLED
              value: "false"
          volumeMounts:
            - name: workflows
              mountPath: /import/workflows
              readOnly: true
      volumes:
        - name: workflows
          configMap:
            name: ${CM_NAME}
EOF

# Each publish:workflow boots the n8n CLI (~5–15s); 36 imports + 36 publishes can take >10 min on Homelab.
kubectl -n "${NS}" wait --for=condition=complete "job/${JOB_NAME}" --timeout=900s

trap - EXIT
restore_n8n

echo "--- Verify workflow list (sample) ---"
kubectl -n "${NS}" exec deploy/"${N8N_DEPLOY}" -- n8n list:workflow 2>/dev/null | grep -E 'osbWf(Osb|Git|Redis|Kc)' | head -20 || true

echo "=== n8n Homelab seed done ==="
echo "Tip: production webhooks activate after the n8n process starts against published versions."
echo "Probe e.g.: kubectl -n ${NS} exec deploy/${N8N_DEPLOY} -- wget -qO- http://127.0.0.1:5678/healthz"
