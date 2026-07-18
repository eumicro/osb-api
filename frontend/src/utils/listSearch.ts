/**
 * Clientseitige Listensuche über id / title / description.
 * Title: `title`, `name` oder `displayName` (je nach Entity).
 */

function asRecord(item: unknown): Record<string, unknown> | null {
  if (item == null || typeof item !== "object") return null;
  return item as Record<string, unknown>;
}

function readString(record: Record<string, unknown>, key: string): string {
  const value = record[key];
  return typeof value === "string" ? value : "";
}

/** Extrahiert die Suchfelder eines Listeneintrags. */
export function listSearchFields(item: unknown): { id: string; title: string; description: string } {
  const record = asRecord(item);
  if (!record) {
    return { id: "", title: "", description: "" };
  }
  const title =
    readString(record, "title")
    || readString(record, "name")
    || readString(record, "displayName");
  return {
    id: readString(record, "id"),
    title,
    description: readString(record, "description"),
  };
}

export function matchesListSearch(item: unknown, query: string): boolean {
  const needle = query.trim().toLowerCase();
  if (!needle) return true;
  const fields = listSearchFields(item);
  return [fields.id, fields.title, fields.description].some((value) =>
    value.toLowerCase().includes(needle),
  );
}

export function filterByListSearch<T>(items: readonly T[], query: string): T[] {
  const needle = query.trim();
  if (!needle) return [...items];
  return items.filter((item) => matchesListSearch(item, needle));
}
