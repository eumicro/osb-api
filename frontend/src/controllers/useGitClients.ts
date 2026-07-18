import { onMounted, ref, watch } from "vue";
import type { CreateGitClientInstanceRequest, GitClientInstance } from "../models/gitClient";
import { gitClientService } from "../services/gitClientService";
import { notifyGitClientsChanged, workspace } from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Git-Client-Liste (serverseitig paginiert). */
export function useGitClients() {
  const gitClients = ref<GitClientInstance[]>([]);
  const page = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const result = await run(() =>
      gitClientService.list({ page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      gitClients.value = result.items;
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
    request: CreateGitClientInstanceRequest,
  ): Promise<GitClientInstance | undefined> {
    const saved = await run(() => gitClientService.create(request));
    if (saved) {
      notifyGitClientsChanged();
      await load();
    }
    return saved;
  }

  async function remove(id: string) {
    await run(() => gitClientService.delete(id));
    notifyGitClientsChanged();
    await load();
  }

  onMounted(() => {
    void load();
  });

  watch(
    () => workspace.gitClientRevision,
    () => {
      void load();
    },
  );

  return {
    gitClients,
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
