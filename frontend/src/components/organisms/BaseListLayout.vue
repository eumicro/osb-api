<script setup lang="ts" generic="T">
import { computed, ref, watch } from "vue";
import { usePagination } from "../../controllers/usePagination";
import { ADMIN_LOOKUP_PAGE_SIZE } from "../../models/page";
import { filterByListSearch } from "../../utils/listSearch";
import SearchField from "../atoms/SearchField.vue";
import ErrorMessage from "../molecules/ErrorMessage.vue";
import PanelToolbar from "../molecules/PanelToolbar.vue";
import PaginationBar from "../molecules/PaginationBar.vue";

/**
 * Organism: gemeinsames List-Layout (Hint, Toolbar inkl. Suche, Body, Pagination).
 * - mode=client: sliced lokal aus gefilterten `items`
 * - mode=server: bei aktiver Suche mehr Einträge laden und clientseitig filtern
 */
const props = withDefaults(
  defineProps<{
    items: readonly T[];
    error?: string;
    hint?: string;
    empty?: string;
    loading?: boolean;
    ready?: boolean;
    mode?: "client" | "server";
    page?: number;
    pageSize?: number;
    total?: number;
    pageCount?: number;
    searchable?: boolean;
  }>(),
  {
    ready: true,
    loading: false,
    mode: "client",
    searchable: true,
  },
);

const emit = defineEmits<{
  "update:page": [page: number];
  "update:pageSize": [pageSize: number];
}>();

const searchQuery = ref("");
const filteredItems = computed(() => filterByListSearch(props.items, searchQuery.value));
const hasSearch = computed(() => searchQuery.value.trim().length > 0);

const client = usePagination(() => filteredItems.value, {
  initialPageSize: props.pageSize,
});

const isServer = computed(() => props.mode === "server");
/** Server-Pagination nur ohne Suche; mit Suche lokal über die geladene Seite. */
const useLocalPaging = computed(() => !isServer.value || hasSearch.value);

const viewPage = computed(() =>
  useLocalPaging.value ? client.page.value : (props.page ?? 1),
);
const viewPageSize = computed(() =>
  useLocalPaging.value
    ? client.pageSize.value
    : (props.pageSize ?? 10),
);
const viewTotal = computed(() =>
  useLocalPaging.value
    ? client.total.value
    : (props.total ?? props.items.length),
);
const viewPageCount = computed(() => {
  if (useLocalPaging.value) return client.pageCount.value;
  if (props.pageCount != null) return props.pageCount;
  const size = viewPageSize.value || 1;
  return Math.max(1, Math.ceil(viewTotal.value / size) || 1);
});
const viewItems = computed(() =>
  useLocalPaging.value ? client.pageItems.value : [...props.items],
);
const rangeStart = computed(() =>
  viewTotal.value === 0 ? 0 : (viewPage.value - 1) * viewPageSize.value + 1,
);
const rangeEnd = computed(() =>
  Math.min(viewPage.value * viewPageSize.value, viewTotal.value),
);
const canPrev = computed(() => viewPage.value > 1);
const canNext = computed(() => viewPage.value < viewPageCount.value);

const savedServerPageSize = ref<number | null>(null);

watch(searchQuery, () => {
  client.page.value = 1;
});

watch(hasSearch, (active) => {
  if (!isServer.value) return;
  if (active) {
    if (savedServerPageSize.value == null) {
      savedServerPageSize.value = props.pageSize ?? 10;
    }
    if ((props.pageSize ?? 10) < ADMIN_LOOKUP_PAGE_SIZE) {
      emit("update:pageSize", ADMIN_LOOKUP_PAGE_SIZE);
      emit("update:page", 1);
    }
    return;
  }
  if (savedServerPageSize.value != null) {
    const restore = savedServerPageSize.value;
    savedServerPageSize.value = null;
    emit("update:pageSize", restore);
    emit("update:page", 1);
  }
});

watch(
  () => filteredItems.value.length,
  () => {
    if (useLocalPaging.value && client.page.value > client.pageCount.value) {
      client.page.value = client.pageCount.value;
    }
  },
);

function onPrev() {
  if (!canPrev.value) return;
  if (useLocalPaging.value) client.prev();
  else emit("update:page", viewPage.value - 1);
}

function onNext() {
  if (!canNext.value) return;
  if (useLocalPaging.value) client.next();
  else emit("update:page", viewPage.value + 1);
}

function onPageSize(size: number) {
  if (useLocalPaging.value) {
    client.setPageSize(size);
    return;
  }
  emit("update:pageSize", size);
  emit("update:page", 1);
}
</script>

<template>
  <div class="base-list">
    <p v-if="!ready && !loading && empty" class="muted">{{ empty }}</p>
    <p v-else-if="loading && !ready" class="muted">{{ $t("common.loading") }}</p>
    <template v-else-if="ready">
      <p v-if="hint" class="base-list__hint muted">{{ hint }}</p>
      <PanelToolbar v-if="searchable || $slots.toolbar || $slots['toolbar-end']">
        <slot name="toolbar" />
        <template #end>
          <SearchField v-if="searchable" v-model="searchQuery" />
          <slot name="toolbar-end" />
        </template>
      </PanelToolbar>
      <ErrorMessage :message="error ?? ''" />
      <slot name="before" />
      <div class="base-list__body">
        <slot :items="viewItems" />
      </div>
      <footer class="base-list__footer">
        <PaginationBar
          :page="viewPage"
          :page-count="viewPageCount"
          :page-size="viewPageSize"
          :total="viewTotal"
          :range-start="rangeStart"
          :range-end="rangeEnd"
          :can-prev="canPrev"
          :can-next="canNext"
          @prev="onPrev"
          @next="onNext"
          @update:page-size="onPageSize"
        />
      </footer>
      <slot name="dialog" />
    </template>
    <ErrorMessage v-else :message="error ?? ''" />
  </div>
</template>

<style scoped>
.base-list {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.base-list__hint {
  margin: 0 0 0.5rem;
  flex: 0 0 auto;
}

.base-list__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.base-list__footer {
  flex: 0 0 auto;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border);
}
</style>
