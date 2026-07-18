import { onMounted, ref, watch } from "vue";
import type { AdminCatalog } from "../models/catalog";
import type { CreateCatalogRequest } from "../models/catalogAdmin";
import { ADMIN_LOOKUP_PAGE_SIZE } from "../models/page";
import { catalogAdminService } from "../services/catalogAdminService";
import { notifyCatalogsChanged, workspace } from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Katalog-Liste (optional serverseitig paginiert). */
export function useCatalogs(options: { paged?: boolean } = {}) {
  const paged = options.paged ?? false;
  const catalogs = ref<AdminCatalog[]>([]);
  const page = ref(1);
  const pageSize = ref(paged ? 10 : ADMIN_LOOKUP_PAGE_SIZE);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const result = await run(() =>
      catalogAdminService.listCatalogs({ page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      catalogs.value = result.items;
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

  async function create(request: CreateCatalogRequest): Promise<AdminCatalog | undefined> {
    const saved = await run(() => catalogAdminService.createCatalog(request));
    if (saved) {
      notifyCatalogsChanged();
      await load();
    }
    return saved;
  }

  async function remove(catalogId: string) {
    await run(() => catalogAdminService.deleteCatalog(catalogId));
    notifyCatalogsChanged();
    await load();
  }

  onMounted(() => {
    void load();
  });

  watch(
    () => workspace.catalogRevision,
    () => {
      void load();
    },
  );

  return {
    catalogs,
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
