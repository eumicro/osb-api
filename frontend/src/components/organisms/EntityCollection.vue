<script setup lang="ts" generic="T">
import { computed } from "vue";
import { useCollectionTableState } from "../../controllers/useCollectionTableState";
import type { CollectionColumn, CollectionViewMode, SortDirection } from "../../models/collectionView";
import {
  columnKey,
  findColumn,
  groupCollectionItems,
  nextSortDirection,
  sortCollectionItems,
} from "../../utils/collectionTable";
import DeleteButton from "../molecules/DeleteButton.vue";
import OpenInNewTabButton from "../molecules/OpenInNewTabButton.vue";
import TableColumnHeader from "../molecules/TableColumnHeader.vue";

const props = withDefaults(
  defineProps<{
    items: T[];
    columns: CollectionColumn<T>[];
    mode: CollectionViewMode;
    /** Persistenz-Scope für Sortierung/Gruppierung (ui-config). */
    scope: string;
    itemKey: (item: T) => string;
    selectedKey: string | null;
    emptyLabel: string;
    canRemove?: (item: T) => boolean;
  }>(),
  { canRemove: () => true },
);

const emit = defineEmits<{
  select: [item: T];
  remove: [item: T];
  openInTab: [item: T];
}>();

const { sortKey, sortDirection, groupKey, setSort, setGroupKey } = useCollectionTableState(
  props.scope,
);

const primaryColumn = computed(
  () => props.columns.find((column) => column.primary) ?? props.columns[0],
);

const detailColumns = computed(() =>
  props.columns.filter((column) => column !== primaryColumn.value),
);

const sortedItems = computed(() =>
  sortCollectionItems(
    props.items,
    findColumn(props.columns, sortKey.value),
    sortDirection.value,
  ),
);

const groupedRows = computed(() =>
  groupCollectionItems(sortedItems.value, findColumn(props.columns, groupKey.value)),
);

function isSelected(item: T): boolean {
  return props.selectedKey != null && props.itemKey(item) === props.selectedKey;
}

function allowRemove(item: T): boolean {
  return props.canRemove(item);
}

function onSort(column: CollectionColumn<T>, index: number) {
  if (column.sortable === false) return;
  const key = columnKey(column, index);
  if (sortKey.value !== key) {
    setSort(key, "asc");
    return;
  }
  const next = nextSortDirection(sortDirection.value);
  setSort(next == null ? null : key, next);
}

function onGroup(column: CollectionColumn<T>, index: number) {
  if (column.groupable === false) return;
  const key = columnKey(column, index);
  setGroupKey(groupKey.value === key ? null : key);
}

function sortDirectionFor(column: CollectionColumn<T>, index: number): SortDirection | null {
  return sortKey.value === columnKey(column, index) ? sortDirection.value : null;
}

function isGrouped(column: CollectionColumn<T>, index: number): boolean {
  return groupKey.value === columnKey(column, index);
}
</script>

<template>
  <div class="entity-collection">
    <table v-if="mode === 'table'" class="data-table">
      <thead>
        <tr>
          <th v-for="(column, index) in columns" :key="columnKey(column, index)">
            <TableColumnHeader
              :label="column.header"
              :sort-direction="sortDirectionFor(column, index)"
              :grouped="isGrouped(column, index)"
              :sortable="column.sortable !== false"
              :groupable="column.groupable !== false"
              @sort="onSort(column, index)"
              @group="onGroup(column, index)"
            />
          </th>
          <th>{{ $t("common.actions") }}</th>
        </tr>
      </thead>
      <tbody v-if="groupedRows">
        <template v-for="group in groupedRows" :key="group.key">
          <tr class="data-table__group-row">
            <td :colspan="columns.length + 1">
              <span class="data-table__group-label">{{ group.label }}</span>
              <span class="data-table__group-count muted">
                {{ $t("collectionTable.groupCount", { count: group.items.length }) }}
              </span>
            </td>
          </tr>
          <tr
            v-for="item in group.items"
            :key="itemKey(item)"
            class="selectable"
            :class="{ selected: isSelected(item) }"
            @click="emit('select', item)"
          >
            <td v-for="(column, index) in columns" :key="columnKey(column, index)">
              <component
                v-if="column.cell"
                :is="column.cell.component"
                v-bind="column.cell.props(item)"
              />
              <template v-else>{{ column.value(item) }}</template>
            </td>
            <td class="actions-cell" @click.stop>
              <OpenInNewTabButton @click="emit('openInTab', item)" />
              <DeleteButton
                :label="$t('common.delete')"
                :disabled="!allowRemove(item)"
                @click="emit('remove', item)"
              />
            </td>
          </tr>
        </template>
      </tbody>
      <tbody v-else>
        <tr
          v-for="item in sortedItems"
          :key="itemKey(item)"
          class="selectable"
          :class="{ selected: isSelected(item) }"
          @click="emit('select', item)"
        >
          <td v-for="(column, index) in columns" :key="columnKey(column, index)">
            <component
              v-if="column.cell"
              :is="column.cell.component"
              v-bind="column.cell.props(item)"
            />
            <template v-else>{{ column.value(item) }}</template>
          </td>
          <td class="actions-cell" @click.stop>
            <OpenInNewTabButton @click="emit('openInTab', item)" />
            <DeleteButton
              :label="$t('common.delete')"
              :disabled="!allowRemove(item)"
              @click="emit('remove', item)"
            />
          </td>
        </tr>
      </tbody>
    </table>

    <ul v-else-if="mode === 'list'" class="collection-list">
      <li
        v-for="item in items"
        :key="itemKey(item)"
        class="collection-list-item selectable"
        :class="{ selected: isSelected(item) }"
        @click="emit('select', item)"
      >
        <div class="collection-list-item__body">
          <div class="collection-list-item__title">
            {{ primaryColumn?.value(item) }}
          </div>
          <div class="collection-list-item__meta muted">
            <span
              v-for="column in detailColumns"
              :key="column.header"
              class="collection-list-item__meta-item"
            >
              <template v-if="column.cell">
                {{ column.header }}:
                <component :is="column.cell.component" v-bind="column.cell.props(item)" />
              </template>
              <template v-else>{{ column.header }}: {{ column.value(item) }}</template>
            </span>
          </div>
        </div>
        <div class="actions-cell" @click.stop>
          <OpenInNewTabButton @click="emit('openInTab', item)" />
          <DeleteButton
            :label="$t('common.delete')"
            :disabled="!allowRemove(item)"
            @click="emit('remove', item)"
          />
        </div>
      </li>
    </ul>

    <div v-else class="collection-cards">
      <article
        v-for="item in items"
        :key="itemKey(item)"
        class="collection-card selectable"
        :class="{ selected: isSelected(item) }"
        @click="emit('select', item)"
      >
        <h4 class="collection-card__title">{{ primaryColumn?.value(item) }}</h4>
        <dl class="collection-card__fields">
          <div v-for="column in detailColumns" :key="column.header" class="collection-card__field">
            <dt>{{ column.header }}</dt>
            <dd>
              <component
                v-if="column.cell"
                :is="column.cell.component"
                v-bind="column.cell.props(item)"
              />
              <template v-else>{{ column.value(item) }}</template>
            </dd>
          </div>
        </dl>
        <div class="actions-cell" @click.stop>
          <OpenInNewTabButton @click="emit('openInTab', item)" />
          <DeleteButton
            :label="$t('common.delete')"
            :disabled="!allowRemove(item)"
            @click="emit('remove', item)"
          />
        </div>
      </article>
    </div>

    <p v-if="items.length === 0" class="muted">{{ emptyLabel }}</p>
  </div>
</template>

<style scoped>
.actions-cell {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.data-table__group-row td {
  background: var(--accent-soft);
  border-bottom: 1px solid var(--border);
  padding: 0.4rem 0.5rem;
  font-weight: 600;
}

.data-table__group-label {
  margin-right: 0.5rem;
}

.data-table__group-count {
  font-weight: 500;
  font-size: 0.85em;
}

.collection-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.collection-list-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.65rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.4rem;
  background: var(--surface);
  cursor: pointer;
}

.collection-list-item.selected,
.collection-card.selected {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.collection-list-item__title,
.collection-card__title {
  margin: 0 0 0.25rem;
  font-size: 0.95rem;
  font-weight: 600;
}

.collection-list-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.85rem;
  font-size: 0.8rem;
}

.collection-list-item__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}

.collection-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
  gap: 0.75rem;
}

.collection-card {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding: 0.85rem;
  border: 1px solid var(--border);
  border-radius: 0.45rem;
  background: var(--surface);
  cursor: pointer;
}

.collection-card__fields {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  flex: 1;
}

.collection-card__field {
  display: grid;
  gap: 0.1rem;
}

.collection-card__field dt {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--muted);
}

.collection-card__field dd {
  margin: 0;
  font-size: 0.85rem;
  word-break: break-word;
}
</style>
