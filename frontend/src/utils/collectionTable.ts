import type { CollectionColumn, SortDirection } from "../models/collectionView";

export function columnKey<T>(column: CollectionColumn<T>, index: number): string {
  return column.id ?? `col-${index}`;
}

export function findColumn<T>(
  columns: readonly CollectionColumn<T>[],
  key: string | null,
): CollectionColumn<T> | undefined {
  if (key == null) return undefined;
  return columns.find((column, index) => columnKey(column, index) === key);
}

export function sortCollectionItems<T>(
  items: readonly T[],
  column: CollectionColumn<T> | undefined,
  direction: SortDirection | null,
): T[] {
  if (!column || !direction) return [...items];
  const factor = direction === "asc" ? 1 : -1;
  return [...items].sort((a, b) => {
    const left = column.value(a);
    const right = column.value(b);
    return (
      left.localeCompare(right, undefined, { sensitivity: "base", numeric: true }) * factor
    );
  });
}

export interface CollectionGroup<T> {
  key: string;
  label: string;
  items: T[];
}

export function groupCollectionItems<T>(
  items: readonly T[],
  column: CollectionColumn<T> | undefined,
): CollectionGroup<T>[] | null {
  if (!column) return null;
  const map = new Map<string, T[]>();
  for (const item of items) {
    const label = column.value(item) || "—";
    const bucket = map.get(label);
    if (bucket) bucket.push(item);
    else map.set(label, [item]);
  }
  return [...map.entries()]
    .sort(([a], [b]) => a.localeCompare(b, undefined, { sensitivity: "base", numeric: true }))
    .map(([label, groupItems]) => ({ key: label, label, items: groupItems }));
}

/** none → asc → desc → none */
export function nextSortDirection(current: SortDirection | null): SortDirection | null {
  if (current === "asc") return "desc";
  if (current === "desc") return null;
  return "asc";
}
