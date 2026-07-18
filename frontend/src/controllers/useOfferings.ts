import { computed, ref, watch } from "vue";
import type { ServiceOffering } from "../models/catalog";
import type { CreateOfferingRequest } from "../models/catalogAdmin";
import { catalogAdminService } from "../services/catalogAdminService";
import {
  notifyOfferingsChanged,
  selectOffering,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Offerings-Liste im selektierten Katalog (serverseitig paginiert). */
export function useOfferings() {
  const offerings = ref<ServiceOffering[]>([]);
  const page = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();
  const selectedOfferingId = computed(() => workspace.selectedOfferingId);
  const catalogId = computed(() => workspace.selectedCatalogId);

  async function load() {
    const id = catalogId.value;
    if (!id) {
      offerings.value = [];
      total.value = 0;
      pageCount.value = 1;
      return;
    }
    const result = await run(() =>
      catalogAdminService.listOfferings(id, { page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      offerings.value = result.items;
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

  function select(offering: ServiceOffering | null) {
    selectOffering(offering?.id ?? null, offering);
  }

  async function create(request: CreateOfferingRequest): Promise<ServiceOffering | undefined> {
    const id = catalogId.value;
    if (!id) return undefined;
    const saved = await run(() => catalogAdminService.createOffering(id, request));
    if (saved) {
      notifyOfferingsChanged();
      await load();
      selectOffering(saved.id, saved);
    }
    return saved;
  }

  async function remove(offeringId: string) {
    const id = catalogId.value;
    if (!id) return;
    await run(() => catalogAdminService.deleteOffering(id, offeringId));
    if (workspace.selectedOfferingId === offeringId) selectOffering(null);
    notifyOfferingsChanged();
    await load();
  }

  watch(
    () => [catalogId.value, workspace.offeringRevision, workspace.catalogRevision] as const,
    () => {
      page.value = 1;
      void load();
    },
    { immediate: true },
  );

  return {
    offerings,
    page,
    pageSize,
    total,
    pageCount,
    selectedOfferingId,
    catalogId,
    loading,
    error,
    load,
    setPage,
    setPageSize,
    select,
    create,
    remove,
  };
}
