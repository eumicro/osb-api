import { ref } from "vue";
import type { Catalog } from "../models/catalog";
import type { SystemInfo } from "../models/systemInfo";
import { catalogService } from "../services/catalogService";
import { systemInfoService } from "../services/systemInfoService";

/** Controller: dashboard data (system info + OSB catalog). */
export function useDashboard() {
  const info = ref<SystemInfo | null>(null);
  const catalog = ref<Catalog | null>(null);
  const loading = ref(false);
  const error = ref("");

  async function load() {
    loading.value = true;
    error.value = "";
    try {
      const [systemInfo, catalogResponse] = await Promise.all([
        systemInfoService.getInfo(),
        catalogService.getCatalog(),
      ]);
      info.value = systemInfo;
      catalog.value = catalogResponse;
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e);
    } finally {
      loading.value = false;
    }
  }

  return { info, catalog, loading, error, load };
}
