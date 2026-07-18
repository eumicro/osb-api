import { ref, watch, type Ref } from "vue";
import type { PlatformClient, UpdatePlatformClientRequest } from "../models/platformClient";
import { platformClientService } from "../services/platformClientService";
import { applyPlatformSelection } from "../stores/selectionSync";
import {
  notifyPlatformsChanged,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Platform-Client-Detail. */
export function usePlatformDetail(
  platformId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const platform = ref<PlatformClient | null>(null) as Ref<PlatformClient | null>;
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const id = platformId();
    if (!id) {
      platform.value = null;
      if (syncWorkspace) workspace.selectedPlatformClient = null;
      return;
    }
    if (syncWorkspace && workspace.selectedPlatformClient?.id === id) {
      platform.value = workspace.selectedPlatformClient;
    }
    const result = await run(() => platformClientService.get(id));
    if (result) {
      platform.value = result;
      if (syncWorkspace) workspace.selectedPlatformClient = result;
    }
  }

  async function save(request: UpdatePlatformClientRequest): Promise<boolean> {
    const id = platformId();
    if (!id) return false;
    const saved = await run(() => platformClientService.update(id, request));
    if (saved) {
      platform.value = saved;
      if (syncWorkspace) {
        workspace.selectedPlatformClientId = saved.id;
        workspace.selectedPlatformClient = saved;
        if (workspace.selectedCatalogId !== saved.catalogId) {
          workspace.selectedCatalogId = saved.catalogId;
          if (workspace.selectedCatalog?.id !== saved.catalogId) {
            workspace.selectedCatalog = null;
          }
        }
      }
      notifyPlatformsChanged();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const id = platformId();
    if (!id) return false;
    await run(() => platformClientService.delete(id));
    platform.value = null;
    if (syncWorkspace) applyPlatformSelection(null);
    notifyPlatformsChanged();
    return error.value === "";
  }

  watch(
    () => [platformId(), workspace.platformRevision, workspace.catalogRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return { platform, loading, error, load, save, remove };
}
