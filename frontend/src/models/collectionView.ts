import type { Component } from "vue";

export type CollectionViewMode = "table" | "list" | "card";

export type SortDirection = "asc" | "desc";

export const COLLECTION_VIEW_MODES: CollectionViewMode[] = ["table", "list", "card"];

export const COLLECTION_VIEW_MODES_KEY = "collection-view-modes";

export const COLLECTION_TABLE_STATE_KEY = "collection-table-state";

export interface CollectionTableState {
  sortKey: string | null;
  sortDirection: SortDirection | null;
  groupKey: string | null;
}

export function isCollectionViewMode(value: unknown): value is CollectionViewMode {
  return value === "table" || value === "list" || value === "card";
}

export function isSortDirection(value: unknown): value is SortDirection {
  return value === "asc" || value === "desc";
}

export function parseCollectionTableState(value: unknown): CollectionTableState | null {
  if (!value || typeof value !== "object") return null;
  const record = value as Record<string, unknown>;
  const sortKey = record.sortKey == null ? null : String(record.sortKey);
  const groupKey = record.groupKey == null ? null : String(record.groupKey);
  const sortDirection =
    record.sortDirection == null
      ? null
      : isSortDirection(record.sortDirection)
        ? record.sortDirection
        : null;
  if (sortKey != null && sortDirection == null) {
    return { sortKey: null, sortDirection: null, groupKey };
  }
  return { sortKey, sortDirection, groupKey };
}

export interface CollectionColumn<T> {
  header: string;
  value: (item: T) => string;
  /** Stable id for sort/group; defaults to column index key. */
  id?: string;
  /** Title line in list/card modes. Defaults to the first column. */
  primary?: boolean;
  /** Default true in table mode. */
  sortable?: boolean;
  /** Default true in table mode. */
  groupable?: boolean;
  /** Optional custom cell (e.g. status icon); falls back to text `value`. */
  cell?: {
    component: Component;
    props: (item: T) => Record<string, unknown>;
  };
}
