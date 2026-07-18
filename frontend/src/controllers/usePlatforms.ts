import { onMounted, ref, watch } from "vue";
import type { CreatePlatformClientRequest, PlatformClient } from "../models/platformClient";
import { ADMIN_LOOKUP_PAGE_SIZE } from "../models/page";
import { platformClientService } from "../services/platformClientService";
import { notifyPlatformsChanged, workspace } from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Platform-Client-Liste (optional serverseitig paginiert). */
export function usePlatforms(options: { paged?: boolean } = {}) {
  const paged = options.paged ?? false;
  const platforms = ref<PlatformClient[]>([]);
  const page = ref(1);
  const pageSize = ref(paged ? 10 : ADMIN_LOOKUP_PAGE_SIZE);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const result = await run(() =>
      platformClientService.list({ page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      platforms.value = result.items;
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

  async function create(request: CreatePlatformClientRequest): Promise<PlatformClient | undefined> {
    const saved = await run(() => platformClientService.create(request));
    if (saved) {
      notifyPlatformsChanged();
      await load();
    }
    return saved;
  }

  async function remove(id: string) {
    await run(() => platformClientService.delete(id));
    notifyPlatformsChanged();
    await load();
  }

  onMounted(() => {
    void load();
  });

  watch(
    () => [workspace.platformRevision, workspace.catalogRevision] as const,
    () => {
      void load();
    },
  );

  return {
    platforms,
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
