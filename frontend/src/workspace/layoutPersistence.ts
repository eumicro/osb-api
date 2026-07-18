import type { DockviewApi } from "dockview-vue";
import { PANELS, buildDefaultLayout, panelTitle } from "./panels";

type LayoutPanels = Record<string, { contentComponent?: string }>;

const KNOWN_COMPONENTS = new Set(PANELS.map((panel) => panel.component));

export function isDynamicPanelId(panelId: string): boolean {
  return (
    panelId.startsWith("catalog:")
    || panelId.startsWith("offering:")
    ||     panelId.startsWith("plan:")
    || panelId.startsWith("instance:")
    || panelId.startsWith("platform:")
  );
}

function isKnownPanel(panelId: string, contentComponent: string | undefined): boolean {
  if (contentComponent && KNOWN_COMPONENTS.has(contentComponent)) return true;
  if (isDynamicPanelId(panelId)) return true;
  return PANELS.some((panel) => panel.id === panelId);
}

function panelEntries(layout: object): Array<[string, { contentComponent?: string }]> {
  const panels = (layout as { panels?: LayoutPanels }).panels;
  if (!panels || typeof panels !== "object") return [];
  return Object.entries(panels);
}

/**
 * Restores a persisted dockview layout.
 * @returns `exact` when fromJSON worked; `fallback` when panels were rebuilt; `default` for empty/default.
 */
export function restoreLayout(
  api: DockviewApi,
  layout: object | null | undefined,
): "exact" | "fallback" | "default" {
  const entries = layout ? panelEntries(layout) : [];
  if (entries.length === 0) {
    api.clear();
    buildDefaultLayout(api);
    return "default";
  }

  const known = entries.filter(([panelId, panel]) =>
    isKnownPanel(panelId, panel?.contentComponent),
  );
  const hasUnknown = known.length !== entries.length;

  if (!hasUnknown) {
    try {
      // fromJSON clears internally — do not call clear() first.
      api.fromJSON(layout as Parameters<DockviewApi["fromJSON"]>[0]);
      return "exact";
    } catch {
      // continue with best-effort rebuild
    }
  }

  api.clear();
  const staticIds = known
    .map(([panelId]) => panelId)
    .filter((panelId) => !isDynamicPanelId(panelId));

  for (const panelId of staticIds) {
    const definition = PANELS.find((panel) => panel.id === panelId);
    if (!definition) continue;
    api.addPanel({
      id: definition.id,
      component: definition.component,
      title: panelTitle(definition.titleKey),
    });
  }

  if (api.panels.length === 0) {
    buildDefaultLayout(api);
    return "default";
  }
  return "fallback";
}
