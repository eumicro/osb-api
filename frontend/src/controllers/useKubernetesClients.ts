import { onMounted, ref, watch } from "vue";
import type {
  CreateKubernetesClientInstanceRequest,
  KubernetesClientInstance,
} from "../models/kubernetesClient";
import { kubernetesClientService } from "../services/kubernetesClientService";
import { notifyKubernetesClientsChanged, workspace } from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Kubernetes-Client-Liste (serverseitig paginiert). */
export function useKubernetesClients() {
  const kubernetesClients = ref<KubernetesClientInstance[]>([]);
  const page = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const result = await run(() =>
      kubernetesClientService.list({ page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      kubernetesClients.value = result.items;
      page.value = result.page;
      pageSize.value = result.pageSize;
      total.value = result.total;
      pageCount.value = result.pageCount;
    }
  }

  function setPage(next: number) {
    page.value = next;
    void load();
  }

  function setPageSize(size: number) {
    pageSize.value = size;
    page.value = 1;
    void load();
  }

  async function create(
    request: CreateKubernetesClientInstanceRequest,
  ): Promise<KubernetesClientInstance | undefined> {
    const saved = await run(() => kubernetesClientService.create(request));
    if (saved) {
      notifyKubernetesClientsChanged();
      await load();
    }
    return saved;
  }

  async function remove(id: string) {
    await run(() => kubernetesClientService.delete(id));
    notifyKubernetesClientsChanged();
    await load();
  }

  onMounted(() => {
    void load();
  });

  watch(
    () => workspace.kubernetesClientRevision,
    () => {
      void load();
    },
  );

  return {
    kubernetesClients,
    page,
    pageSize,
    total,
    pageCount,
    loading,
    error,
    load,
    setPage,
    setPageSize,
    create,
    remove,
  };
}
