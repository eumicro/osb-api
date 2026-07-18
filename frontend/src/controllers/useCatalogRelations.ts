import { ref, watch } from "vue";
import type { AdminCatalog } from "../models/catalog";
import type { CatalogSavePayload, UpdateCatalogRequest } from "../models/catalogAdmin";
import type { PlatformClient } from "../models/platformClient";
import { ADMIN_LOOKUP_PAGE_SIZE } from "../models/page";
import { catalogAdminService } from "../services/catalogAdminService";
import { platformClientService } from "../services/platformClientService";
import { notifyPlatformsChanged, workspace } from "../stores/workspace";

/**
 * Controller: Katalog↔Platform-Zuordnung (Listen + Assign/Unassign beim Speichern).
 */
export function useCatalogRelations(
  catalogId: () => string | null,
  options: {
    saveCatalog: (request: UpdateCatalogRequest) => Promise<boolean>;
    unassignNeedsFallbackMessage: () => string;
    genericErrorMessage: () => string;
  },
) {
  const platforms = ref<PlatformClient[]>([]);
  const catalogs = ref<AdminCatalog[]>([]);
  const relationError = ref("");

  async function load() {
    try {
      const lookup = { page: 1, pageSize: ADMIN_LOOKUP_PAGE_SIZE };
      const [platformList, catalogList] = await Promise.all([
        platformClientService.list(lookup),
        catalogAdminService.listCatalogs(lookup),
      ]);
      platforms.value = platformList.items;
      catalogs.value = catalogList.items;
    } catch {
      platforms.value = [];
      catalogs.value = [];
    }
  }

  async function saveWithAssignments(payload: CatalogSavePayload): Promise<boolean> {
    const id = catalogId();
    if (!id) return false;
    relationError.value = "";

    const assigned = new Set(payload.assignedPlatformIds);
    const toUnassign = platforms.value.filter(
      (platform) => platform.catalogId === id && !assigned.has(platform.id),
    );
    if (toUnassign.length > 0 && !payload.fallbackCatalogId) {
      relationError.value = options.unassignNeedsFallbackMessage();
      return false;
    }

    const catalogSaved = await options.saveCatalog(payload.catalog);
    if (!catalogSaved) return false;

    try {
      for (const platform of platforms.value) {
        const shouldAssign = assigned.has(platform.id);
        const isAssigned = platform.catalogId === id;
        if (shouldAssign && !isAssigned) {
          await platformClientService.update(platform.id, {
            displayName: platform.displayName,
            username: platform.username,
            catalogId: id,
            enabled: platform.enabled,
          });
        } else if (!shouldAssign && isAssigned && payload.fallbackCatalogId) {
          await platformClientService.update(platform.id, {
            displayName: platform.displayName,
            username: platform.username,
            catalogId: payload.fallbackCatalogId,
            enabled: platform.enabled,
          });
        }
      }
      notifyPlatformsChanged();
      await load();
      return true;
    } catch (err) {
      relationError.value =
        err instanceof Error ? err.message : options.genericErrorMessage();
      return false;
    }
  }

  watch(
    () => [catalogId(), workspace.platformRevision, workspace.catalogRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return {
    platforms,
    catalogs,
    relationError,
    load,
    saveWithAssignments,
  };
}
