import { computed, onMounted, ref, type WritableComputedRef } from "vue";
import {
  COLLECTION_VIEW_MODES_KEY,
  isCollectionViewMode,
  type CollectionViewMode,
} from "../models/collectionView";
import { uiConfigService } from "../services/uiConfigService";

const modes = ref<Record<string, CollectionViewMode>>({});
let loadPromise: Promise<void> | null = null;

async function ensureLoaded() {
  if (loadPromise) {
    await loadPromise;
    return;
  }
  loadPromise = (async () => {
    try {
      const saved = await uiConfigService.load<Record<string, unknown>>(COLLECTION_VIEW_MODES_KEY);
      if (!saved || typeof saved !== "object") return;
      const next: Record<string, CollectionViewMode> = {};
      for (const [key, value] of Object.entries(saved)) {
        if (isCollectionViewMode(value)) {
          next[key] = value;
        }
      }
      modes.value = next;
    } catch {
      // keep defaults
    }
  })();
  await loadPromise;
}

async function persist() {
  try {
    await uiConfigService.save(COLLECTION_VIEW_MODES_KEY, modes.value);
  } catch {
    // ignore persistence failures in UI
  }
}

/** Per-list-panel view mode (table / list / card), persisted via BFF ui-config. */
export function useCollectionViewMode(
  scope: string,
  fallback: CollectionViewMode = "table",
): {
  mode: WritableComputedRef<CollectionViewMode>;
} {
  onMounted(() => {
    void ensureLoaded();
  });

  const mode = computed({
    get: () => modes.value[scope] ?? fallback,
    set: (value: CollectionViewMode) => {
      modes.value = { ...modes.value, [scope]: value };
      void persist();
    },
  });

  return { mode };
}
