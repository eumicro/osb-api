import { ref, watch, type Ref } from "vue";
import type { AdminCatalog } from "../models/catalog";
import type { UpdateCatalogRequest } from "../models/catalogAdmin";
import { catalogAdminService } from "../services/catalogAdminService";
import { notifyCatalogsChanged, workspace } from "../stores/workspace";
import { applyCatalogSelection } from "../stores/selectionSync";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: einzelnes Katalog-Detail (Workspace-Selektion oder gebundener Tab). */
export function useCatalogDetail(
  catalogId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const catalog = ref<AdminCatalog | null>(null) as Ref<AdminCatalog | null>;
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const id = catalogId();
    if (!id) {
      catalog.value = null;
      if (syncWorkspace) workspace.selectedCatalog = null;
      return;
    }
    if (syncWorkspace && workspace.selectedCatalog?.id === id) {
      catalog.value = workspace.selectedCatalog;
    }
    const result = await run(() => catalogAdminService.getCatalog(id));
    if (result) {
      catalog.value = result;
      if (syncWorkspace) workspace.selectedCatalog = result;
    }
  }

  async function save(request: UpdateCatalogRequest): Promise<boolean> {
    const id = catalogId();
    if (!id) return false;
    const saved = await run(() => catalogAdminService.updateCatalog(id, request));
    if (saved) {
      catalog.value = saved;
      if (syncWorkspace) {
        workspace.selectedCatalogId = saved.id;
        workspace.selectedCatalog = saved;
      }
      notifyCatalogsChanged();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const id = catalogId();
    if (!id) return false;
    await run(() => catalogAdminService.deleteCatalog(id));
    catalog.value = null;
    if (syncWorkspace) applyCatalogSelection(null);
    notifyCatalogsChanged();
    return error.value === "";
  }

  watch(
    () => [catalogId(), workspace.catalogRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return { catalog, loading, error, load, save, remove };
}
