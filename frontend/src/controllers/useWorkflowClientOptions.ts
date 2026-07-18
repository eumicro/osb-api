import { onMounted, ref, watch } from "vue";
import type { GitClientInstance } from "../models/gitClient";
import type { HttpClientInstance } from "../models/httpClient";
import type { KubernetesClientInstance } from "../models/kubernetesClient";
import { ADMIN_LOOKUP_PAGE_SIZE } from "../models/page";
import { gitClientService } from "../services/gitClientService";
import { httpClientService } from "../services/httpClientService";
import { kubernetesClientService } from "../services/kubernetesClientService";
import { workspace } from "../stores/workspace";

/**
 * Controller: HTTP/K8s/Git-Client-Listen für Workflow-Formulare
 * (Create + Detail), inkl. Workspace-Revision-Reload.
 */
export function useWorkflowClientOptions() {
  const httpClients = ref<HttpClientInstance[]>([]);
  const kubernetesClients = ref<KubernetesClientInstance[]>([]);
  const gitClients = ref<GitClientInstance[]>([]);

  async function load() {
    const lookup = { page: 1, pageSize: ADMIN_LOOKUP_PAGE_SIZE };
    const [http, kubernetes, git] = await Promise.all([
      httpClientService.list(lookup).catch(() => null),
      kubernetesClientService.list(lookup).catch(() => null),
      gitClientService.list(lookup).catch(() => null),
    ]);
    httpClients.value = http?.items ?? [];
    kubernetesClients.value = kubernetes?.items ?? [];
    gitClients.value = git?.items ?? [];
  }

  onMounted(() => {
    void load();
  });

  watch(
    () =>
      [
        workspace.httpClientRevision,
        workspace.kubernetesClientRevision,
        workspace.gitClientRevision,
      ] as const,
    () => {
      void load();
    },
  );

  return { httpClients, kubernetesClients, gitClients, load };
}
