import { onMounted, ref, watch, type Ref } from "vue";
import {
  COLLECTION_TABLE_STATE_KEY,
  parseCollectionTableState,
  type CollectionTableState,
  type SortDirection,
} from "../models/collectionView";
import { uiConfigService } from "../services/uiConfigService";

const EMPTY: CollectionTableState = {
  sortKey: null,
  sortDirection: null,
  groupKey: null,
};

const states = ref<Record<string, CollectionTableState>>({});
let loadPromise: Promise<void> | null = null;

async function ensureLoaded() {
  if (loadPromise) {
    await loadPromise;
    return;
  }
  loadPromise = (async () => {
    try {
      const saved = await uiConfigService.load<Record<string, unknown>>(COLLECTION_TABLE_STATE_KEY);
      if (!saved || typeof saved !== "object") return;
      const next: Record<string, CollectionTableState> = {};
      for (const [key, value] of Object.entries(saved)) {
        const parsed = parseCollectionTableState(value);
        if (parsed) next[key] = parsed;
      }
      states.value = next;
    } catch {
      // keep defaults
    }
  })();
  await loadPromise;
}

async function persist() {
  try {
    await uiConfigService.save(COLLECTION_TABLE_STATE_KEY, states.value);
  } catch {
    // ignore persistence failures in UI
  }
}

function readScope(scope: string): CollectionTableState {
  return states.value[scope] ?? EMPTY;
}

function writeScope(scope: string, patch: Partial<CollectionTableState>) {
  const current = readScope(scope);
  const next: CollectionTableState = {
    sortKey: patch.sortKey !== undefined ? patch.sortKey : current.sortKey,
    sortDirection:
      patch.sortDirection !== undefined ? patch.sortDirection : current.sortDirection,
    groupKey: patch.groupKey !== undefined ? patch.groupKey : current.groupKey,
  };
  if (next.sortKey == null) next.sortDirection = null;
  if (next.sortDirection == null) next.sortKey = null;
  states.value = { ...states.value, [scope]: next };
  void persist();
}

/** Per-list-panel table sort/group state, persisted via BFF ui-config. */
export function useCollectionTableState(scope: string): {
  sortKey: Ref<string | null>;
  sortDirection: Ref<SortDirection | null>;
  groupKey: Ref<string | null>;
  setSort: (sortKey: string | null, sortDirection: SortDirection | null) => void;
  setGroupKey: (groupKey: string | null) => void;
} {
  const sortKey = ref<string | null>(readScope(scope).sortKey);
  const sortDirection = ref<SortDirection | null>(readScope(scope).sortDirection);
  const groupKey = ref<string | null>(readScope(scope).groupKey);

  onMounted(() => {
    void ensureLoaded().then(() => {
      const saved = readScope(scope);
      sortKey.value = saved.sortKey;
      sortDirection.value = saved.sortDirection;
      groupKey.value = saved.groupKey;
    });
  });

  watch(
    () => states.value[scope],
    (saved) => {
      const next = saved ?? EMPTY;
      sortKey.value = next.sortKey;
      sortDirection.value = next.sortDirection;
      groupKey.value = next.groupKey;
    },
  );

  function setSort(nextKey: string | null, nextDirection: SortDirection | null) {
    sortKey.value = nextKey;
    sortDirection.value = nextDirection;
    writeScope(scope, { sortKey: nextKey, sortDirection: nextDirection });
  }

  function setGroupKey(nextKey: string | null) {
    groupKey.value = nextKey;
    writeScope(scope, { groupKey: nextKey });
  }

  return { sortKey, sortDirection, groupKey, setSort, setGroupKey };
}
