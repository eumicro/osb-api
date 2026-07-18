import { ref, watch, type Ref } from "vue";
import type {
  KubernetesClientInstance,
  UpdateKubernetesClientInstanceRequest,
} from "../models/kubernetesClient";
import { kubernetesClientService } from "../services/kubernetesClientService";
import {
  notifyKubernetesClientsChanged,
  selectKubernetesClient,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

export function useKubernetesClientDetail(
  kubernetesClientId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const kubernetesClient = ref<KubernetesClientInstance | null>(
    null,
  ) as Ref<KubernetesClientInstance | null>;
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const id = kubernetesClientId();
    if (!id) {
      kubernetesClient.value = null;
      if (syncWorkspace) workspace.selectedKubernetesClient = null;
      return;
    }
    if (syncWorkspace && workspace.selectedKubernetesClient?.id === id) {
      kubernetesClient.value = workspace.selectedKubernetesClient;
    }
    const result = await run(() => kubernetesClientService.get(id));
    if (result) {
      kubernetesClient.value = result;
      if (syncWorkspace) workspace.selectedKubernetesClient = result;
    }
  }

  async function save(request: UpdateKubernetesClientInstanceRequest): Promise<boolean> {
    const id = kubernetesClientId();
    if (!id) return false;
    const saved = await run(() => kubernetesClientService.update(id, request));
    if (saved) {
      kubernetesClient.value = saved;
      if (syncWorkspace) selectKubernetesClient(saved.id, saved);
      notifyKubernetesClientsChanged();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const id = kubernetesClientId();
    if (!id) return false;
    await run(() => kubernetesClientService.delete(id));
    kubernetesClient.value = null;
    if (syncWorkspace) selectKubernetesClient(null);
    notifyKubernetesClientsChanged();
    return error.value === "";
  }

  watch(
    () => [kubernetesClientId(), workspace.kubernetesClientRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return { kubernetesClient, loading, error, load, save, remove };
}
