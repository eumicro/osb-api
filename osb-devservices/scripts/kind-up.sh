#!/usr/bin/env bash
# Create (or reuse) the local Kind cluster "osb" for provision tests.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=container-cli.sh
source "${SCRIPT_DIR}/container-cli.sh"

CLUSTER_NAME="${KIND_CLUSTER_NAME:-osb}"
CONFIG="${ROOT}/kind/config.yaml"
KUBECONFIG_OUT="${ROOT}/kind/kubeconfig"
NAMESPACE="${KIND_NAMESPACE:-osb-demo}"

if ! command -v kind >/dev/null 2>&1; then
  echo "WARN: kind not found in PATH — skip Kind cluster." >&2
  echo "      Install: https://kind.sigs.k8s.io/docs/user/quick-start/#installation" >&2
  exit 0
fi

resolve_container_cli
export KIND_EXPERIMENTAL_PROVIDER="${KIND_EXPERIMENTAL_PROVIDER:-${CONTAINER_CLI}}"

ensure_podman_rootful() {
  if [[ "${CONTAINER_CLI}" != "podman" ]]; then
    return 0
  fi
  if ! command -v podman >/dev/null 2>&1; then
    return 0
  fi
  # Kind on Windows/WSL needs a rootful Podman machine (ip6tables otherwise fails).
  local rootless
  rootless="$(podman info --format '{{.Host.Security.Rootless}}' 2>/dev/null || echo true)"
  if [[ "${rootless}" != "true" ]]; then
    echo "Kind provider: podman (rootful)"
    return 0
  fi
  if [[ "${OSB_KIND_AUTO_ROOTFUL:-true}" != "true" ]]; then
    echo "WARN: Podman is rootless; Kind usually fails. Set OSB_KIND_AUTO_ROOTFUL=true or:" >&2
    echo "  podman machine stop && podman machine set --rootful && podman machine start" >&2
    return 0
  fi
  if ! podman machine list --format '{{.Name}}' 2>/dev/null | grep -q .; then
    echo "WARN: no podman machine found — cannot switch to rootful automatically." >&2
    return 0
  fi
  echo "Podman is rootless — switching default machine to rootful for Kind..."
  podman machine stop || true
  podman machine set --rootful
  podman machine start
  echo "Kind provider: podman (rootful, auto-switched)"
}

ensure_podman_rootful

if kind get clusters 2>/dev/null | grep -qx "${CLUSTER_NAME}"; then
  # extraPortMappings are fixed at create time — recreate when ingress host port is missing
  # (redis-ui-*.localhost:8088 → kind node :80 → ingress-nginx).
  ingress_host_mapped=0
  if [[ "${CONTAINER_CLI}" == "podman" ]] || [[ "${CONTAINER_CLI}" == "docker" ]]; then
    if ${CONTAINER_CLI} port "${CLUSTER_NAME}-control-plane" 80 2>/dev/null | grep -q ':8088'; then
      ingress_host_mapped=1
    fi
  fi
  if [[ "${OSB_KIND_FORCE_RECREATE:-}" == "true" ]] || [[ "${ingress_host_mapped}" -ne 1 ]]; then
    echo "Kind cluster '${CLUSTER_NAME}' exists but host port 8088→80 is not mapped — recreating..."
    kind delete cluster --name "${CLUSTER_NAME}"
  else
    echo "Kind cluster '${CLUSTER_NAME}' already exists — reusing."
  fi
fi

if ! kind get clusters 2>/dev/null | grep -qx "${CLUSTER_NAME}"; then
  echo "Creating Kind cluster '${CLUSTER_NAME}'..."
  if ! kind create cluster --name "${CLUSTER_NAME}" --config "${CONFIG}"; then
    echo >&2
    echo "ERROR: Kind cluster creation failed." >&2
    if [[ "${CONTAINER_CLI}" == "podman" ]]; then
      echo "On Windows/WSL, Kind needs a rootful Podman machine:" >&2
      echo "  podman machine stop" >&2
      echo "  podman machine set --rootful" >&2
      echo "  podman machine start" >&2
      echo "Then re-run: ./osb-devservices/scripts/kind-up.sh" >&2
      echo "Or skip Kind for now: OSB_SKIP_KIND=true ./osb-devservices/scripts/up.sh" >&2
    fi
    exit 1
  fi
fi

kind export kubeconfig --name "${CLUSTER_NAME}" --kubeconfig "${KUBECONFIG_OUT}"
chmod 600 "${KUBECONFIG_OUT}" 2>/dev/null || true

if command -v kubectl >/dev/null 2>&1; then
  export KUBECONFIG="${KUBECONFIG_OUT}"
  kubectl cluster-info
  kubectl get nodes
  kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
  echo "Namespace ready: ${NAMESPACE}"

  # Ingress NGINX (Kind): http://redis-ui-<instanceId>.localhost:8088/
  # Requires kind/config.yaml hostPort 8088→80 + ingress-ready label (recreate cluster if missing).
  echo "Ensuring ingress-nginx..."
  kubectl apply -f https://kind.sigs.k8s.io/examples/ingress/deploy-ingress-nginx.yaml
  # Fewer workers avoids pthread_create failures under Podman/WSL resource limits.
  kubectl -n ingress-nginx create configmap ingress-nginx-controller \
    --from-literal=worker-processes=2 \
    --dry-run=client -o yaml | kubectl apply -f -
  kubectl -n ingress-nginx wait --for=condition=ready pod \
    -l app.kubernetes.io/component=controller --timeout=180s \
    && echo "ingress-nginx ready." \
    || echo "WARN: ingress-nginx not ready yet — redis-ui Ingress URLs need it." >&2
  kubectl -n ingress-nginx rollout restart deploy/ingress-nginx-controller >/dev/null 2>&1 || true
  kubectl -n ingress-nginx rollout status deploy/ingress-nginx-controller --timeout=180s >/dev/null 2>&1 \
    || echo "WARN: ingress-nginx rollout not finished yet." >&2

  # ServiceAccount + ClusterRoleBinding for osb-api Kubernetes client (Bearer token).
  SA_NS="${KIND_OSB_SA_NAMESPACE:-kube-system}"
  SA_NAME="${KIND_OSB_SA_NAME:-osb-broker}"
  kubectl -n "${SA_NS}" create serviceaccount "${SA_NAME}" --dry-run=client -o yaml | kubectl apply -f -
  kubectl create clusterrolebinding "${SA_NAME}" \
    --clusterrole=cluster-admin \
    --serviceaccount="${SA_NS}:${SA_NAME}" \
    --dry-run=client -o yaml | kubectl apply -f -

  TOKEN=""
  if TOKEN="$(kubectl -n "${SA_NS}" create token "${SA_NAME}" --duration=8760h 2>/dev/null)"; then
    echo "Created ServiceAccount token for ${SA_NS}/${SA_NAME}"
  else
    echo "WARN: kubectl create token failed — trying legacy secret token." >&2
    SECRET="$(kubectl -n "${SA_NS}" get sa "${SA_NAME}" -o jsonpath='{.secrets[0].name}' 2>/dev/null || true)"
    if [[ -n "${SECRET}" ]]; then
      TOKEN="$(kubectl -n "${SA_NS}" get secret "${SECRET}" -o jsonpath='{.data.token}' | base64 -d 2>/dev/null || true)"
    fi
  fi

  if [[ -n "${TOKEN}" ]]; then
    TOKEN_FILE="${ROOT}/kind/osb-broker.token"
    printf '%s' "${TOKEN}" >"${TOKEN_FILE}"
    chmod 600 "${TOKEN_FILE}" 2>/dev/null || true
    echo "Token written: ${TOKEN_FILE}"

    # Push into OSB Postgres when the DB is already up (Flyway V7 seed client k8s-local-dev).
    PG_CONTAINER="${POSTGRES_CONTAINER:-osb-postgres}"
    PG_USER="${POSTGRES_USER:-osb}"
    PG_DB="${POSTGRES_DB:-osb}"
    if ${CONTAINER_CLI} exec "${PG_CONTAINER}" pg_isready -U "${PG_USER}" -d "${PG_DB}" >/dev/null 2>&1; then
      ESCAPED="$(printf '%s' "${TOKEN}" | sed "s/'/''/g")"
      ${CONTAINER_CLI} exec -i "${PG_CONTAINER}" \
        psql -U "${PG_USER}" -d "${PG_DB}" -v ON_ERROR_STOP=1 \
        -c "UPDATE kubernetes_client_instances
            SET auth_type = 'BEARER',
                token = '${ESCAPED}',
                api_server_url = 'https://host.docker.internal:${KIND_API_PORT:-6443}',
                insecure_skip_tls_verify = TRUE,
                enabled = TRUE
            WHERE id = 'k8s-local-dev';" \
        && echo "Updated kubernetes_client_instances.k8s-local-dev token + host.docker.internal URL in Postgres." \
        || echo "WARN: could not UPDATE k8s-local-dev in Postgres (run after osb-api/Flyway)." >&2
    else
      echo "Postgres ${PG_CONTAINER} not ready — token saved to ${TOKEN_FILE}; apply later."
    fi
  else
    echo "WARN: no Kind ServiceAccount token available for k8s-local-dev." >&2
  fi
else
  echo "WARN: kubectl not found — cluster created, namespace '${NAMESPACE}' not ensured." >&2
fi

echo
echo "Kind kubeconfig : ${KUBECONFIG_OUT}"
echo "API server      : https://127.0.0.1:${KIND_API_PORT:-6443} (host kubectl)"
echo "Broker client   : https://host.docker.internal:${KIND_API_PORT:-6443} (Compose osb-api)"
echo "Namespace       : ${NAMESPACE}"
echo "Export          : export KUBECONFIG=${KUBECONFIG_OUT}"
