import { computed, onMounted, ref, type WritableComputedRef } from "vue";
import { uiConfigService } from "../services/uiConfigService";

export const COLLAPSIBLE_OPEN_KEY = "collapsible-open";

const openByScope = ref<Record<string, boolean>>({});
let loadPromise: Promise<void> | null = null;

async function ensureLoaded() {
  if (loadPromise) {
    await loadPromise;
    return;
  }
  loadPromise = (async () => {
    try {
      const saved = await uiConfigService.load<Record<string, unknown>>(COLLAPSIBLE_OPEN_KEY);
      if (!saved || typeof saved !== "object") return;
      const next: Record<string, boolean> = {};
      for (const [key, value] of Object.entries(saved)) {
        if (typeof value === "boolean") next[key] = value;
      }
      openByScope.value = next;
    } catch {
      // keep defaults
    }
  })();
  await loadPromise;
}

async function persist() {
  try {
    await uiConfigService.save(COLLAPSIBLE_OPEN_KEY, openByScope.value);
  } catch {
    // ignore persistence failures in UI
  }
}

/** Ein-/Ausklapp-Zustand pro Scope, persistiert via BFF ui-config. */
export function useCollapsibleOpen(
  scope: string,
  fallback = true,
): {
  open: WritableComputedRef<boolean>;
} {
  onMounted(() => {
    void ensureLoaded();
  });

  const open = computed({
    get: () => openByScope.value[scope] ?? fallback,
    set: (value: boolean) => {
      openByScope.value = { ...openByScope.value, [scope]: value };
      void persist();
    },
  });

  return { open };
}
