import { onMounted, ref, watch } from "vue";
import type {
  CreateHttpClientInstanceRequest,
  HttpClientInstance,
} from "../models/httpClient";
import { httpClientService } from "../services/httpClientService";
import { notifyHttpClientsChanged, workspace } from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: HTTP-Client-Liste (serverseitig paginiert). */
export function useHttpClients() {
  const httpClients = ref<HttpClientInstance[]>([]);
  const page = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const result = await run(() =>
      httpClientService.list({ page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      httpClients.value = result.items;
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
    request: CreateHttpClientInstanceRequest,
  ): Promise<HttpClientInstance | undefined> {
    const saved = await run(() => httpClientService.create(request));
    if (saved) {
      notifyHttpClientsChanged();
      await load();
    }
    return saved;
  }

  async function remove(id: string) {
    await run(() => httpClientService.delete(id));
    notifyHttpClientsChanged();
    await load();
  }

  onMounted(() => {
    void load();
  });

  watch(
    () => workspace.httpClientRevision,
    () => {
      void load();
    },
  );

  return {
    httpClients,
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
