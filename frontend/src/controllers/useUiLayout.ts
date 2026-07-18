import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { DockviewApi, DockviewReadyEvent } from "dockview-vue";
import type { WorkspaceView, WorkspaceViewsState } from "../models/workspaceView";
import { LEGACY_LAYOUT_KEY, VIEWS_CONFIG_KEY } from "../models/workspaceView";
import {
  exportFilename,
  parseExportDocument,
  toExportDocument,
} from "../models/workspaceViewExport";
import { downloadJson } from "../services/downloadService";
import { uiConfigService } from "../services/uiConfigService";
import { registerDockviewApi } from "../stores/workspaceLayout";
import { restoreLayout } from "../workspace/layoutPersistence";
import {
  PANELS,
  panelTitle,
  refreshPanelTitles,
  VIEW_PRESETS,
  buildDefaultLayout,
} from "../workspace/panels";

const SAVE_DEBOUNCE_MS = 1000;

/**
 * Controller: mehrere benannte Workspace-Ansichten pro Benutzer. Jede Ansicht
 * speichert ein eigenes Dockview-Layout; Wechsel, Anlegen, Umbenennen und
 * Loeschen werden ueber den UI-ConfigService des BFF persistiert.
 */
export function useUiLayout() {
  let api: DockviewApi | null = null;
  let saveTimer: ReturnType<typeof setTimeout> | undefined;
  let layoutApplying = false;
  const { locale } = useI18n({ useScope: "global" });

  const views = ref<WorkspaceView[]>([]);
  const activeViewId = ref<string | null>(null);
  const openPanelIds = ref<string[]>([]);

  const activeView = computed(
    () => views.value.find((view) => view.id === activeViewId.value) ?? null,
  );

  async function onReady(event: DockviewReadyEvent) {
    api = event.api;
    registerDockviewApi(api);
    const { state, migratedFromLegacy } = await loadViewsState();
    views.value = state.views;
    activeViewId.value = state.activeViewId;
    applyLayout(activeView.value?.layout ?? null);
    refreshOpenPanels();
    if (migratedFromLegacy) {
      await flushSave();
      await uiConfigService.remove(LEGACY_LAYOUT_KEY).catch(() => undefined);
    }

    // Checkbox / open-panel state (any layout change).
    api.onDidLayoutChange(() => {
      refreshOpenPanels();
    });

    // Persist only user-driven structural changes — never programmatic fromJSON/clear/addPanel.
    api.onDidMutateLayout((mutation) => {
      if (layoutApplying || mutation.origin !== "user") return;
      scheduleSave();
    });

    watch(locale, () => {
      if (api) refreshPanelTitles(api);
    });
  }

  async function loadViewsState(): Promise<{
    state: WorkspaceViewsState;
    migratedFromLegacy: boolean;
  }> {
    const saved = await uiConfigService
      .load<WorkspaceViewsState>(VIEWS_CONFIG_KEY)
      .catch(() => null);
    if (saved?.views?.length) {
      return { state: saved, migratedFromLegacy: false };
    }

    const legacy = await uiConfigService.load<object>(LEGACY_LAYOUT_KEY).catch(() => null);
    if (legacy) {
      return {
        state: {
          activeViewId: "standard",
          views: [{ id: "standard", name: "Standard", layout: legacy }],
        },
        migratedFromLegacy: true,
      };
    }

    return {
      state: {
        activeViewId: "standard",
        views: [{ id: "standard", name: "Standard", layout: {} }],
      },
      migratedFromLegacy: false,
    };
  }

  function applyLayout(layout: object | null | undefined) {
    if (!api) return;
    layoutApplying = true;
    try {
      restoreLayout(api, layout);
    } finally {
      // Keep the flag briefly so coalesced dockview events after fromJSON stay ignored.
      setTimeout(() => {
        layoutApplying = false;
        refreshOpenPanels();
      }, 100);
    }
  }

  function refreshOpenPanels() {
    openPanelIds.value = api ? api.panels.map((panel) => panel.id) : [];
  }

  function updateActiveViewLayout() {
    if (!api || !activeViewId.value) return;
    const layout = api.toJSON();
    views.value = views.value.map((view) =>
      view.id === activeViewId.value ? { ...view, layout } : view,
    );
  }

  async function persistState() {
    if (!activeViewId.value || views.value.length === 0) return;
    const state: WorkspaceViewsState = {
      activeViewId: activeViewId.value,
      views: views.value,
    };
    await uiConfigService.save(VIEWS_CONFIG_KEY, state);
  }

  async function flushSave() {
    clearTimeout(saveTimer);
    if (!api) return;
    updateActiveViewLayout();
    await persistState().catch(() => undefined);
  }

  function scheduleSave() {
    if (layoutApplying) return;
    clearTimeout(saveTimer);
    saveTimer = setTimeout(() => {
      if (!api || layoutApplying) return;
      updateActiveViewLayout();
      void persistState().catch(() => undefined);
    }, SAVE_DEBOUNCE_MS);
  }

  async function switchView(viewId: string) {
    if (!api || viewId === activeViewId.value) return;
    await flushSave();
    activeViewId.value = viewId;
    applyLayout(activeView.value?.layout ?? null);
    refreshOpenPanels();
    await persistState().catch(() => undefined);
  }

  async function createView(name: string, presetId = "current") {
    if (!api) return;
    const trimmed = name.trim();
    if (!trimmed) return;

    await flushSave();

    const preset = VIEW_PRESETS.find((entry) => entry.id === presetId);
    let layout: object;

    layoutApplying = true;
    try {
      if (presetId === "current") {
        layout = api.toJSON();
      } else if (preset) {
        api.clear();
        preset.build(api);
        layout = api.toJSON();
      } else {
        layout = api.toJSON();
      }
    } finally {
      setTimeout(() => {
        layoutApplying = false;
        refreshOpenPanels();
      }, 100);
    }

    const view: WorkspaceView = {
      id: crypto.randomUUID(),
      name: trimmed,
      layout,
    };

    views.value = [...views.value, view];
    activeViewId.value = view.id;
    refreshOpenPanels();
    await persistState().catch(() => undefined);
  }

  async function renameView(name: string) {
    const trimmed = name.trim();
    if (!trimmed || !activeViewId.value) return;
    views.value = views.value.map((view) =>
      view.id === activeViewId.value ? { ...view, name: trimmed } : view,
    );
    await persistState().catch(() => undefined);
  }

  async function deleteView() {
    if (!api || views.value.length <= 1 || !activeViewId.value) return;

    const remaining = views.value.filter((view) => view.id !== activeViewId.value);
    activeViewId.value = remaining[0].id;
    views.value = remaining;
    applyLayout(activeView.value?.layout ?? null);
    refreshOpenPanels();
    await persistState().catch(() => undefined);
  }

  function togglePanel(panelId: string) {
    if (!api) return;
    const existing = api.getPanel(panelId);
    if (existing) {
      api.removePanel(existing);
    } else {
      const definition = PANELS.find((panel) => panel.id === panelId);
      if (definition) {
        api.addPanel({
          id: definition.id,
          component: definition.component,
          title: panelTitle(definition.titleKey),
        });
      }
    }
    // togglePanel is API-origin; persist explicitly.
    scheduleSave();
  }

  async function resetCurrentView() {
    if (!api || !activeViewId.value) return;
    layoutApplying = true;
    try {
      api.clear();
      buildDefaultLayout(api);
    } finally {
      setTimeout(() => {
        layoutApplying = false;
        refreshOpenPanels();
      }, 100);
    }
    updateActiveViewLayout();
    await persistState().catch(() => undefined);
  }

  async function exportViewsJson() {
    await flushSave();
    if (!activeViewId.value || views.value.length === 0) return;
    const document = toExportDocument({
      activeViewId: activeViewId.value,
      views: views.value,
    });
    downloadJson(exportFilename(activeView.value?.name), document);
  }

  async function importViewsJson(raw: unknown, mode: "replace" | "merge" = "replace") {
    if (!api) return;
    const imported = parseExportDocument(raw);
    await flushSave();

    if (mode === "merge") {
      const mergedViews = imported.views.map((view) => ({
        ...view,
        id: crypto.randomUUID(),
      }));
      views.value = [...views.value, ...mergedViews];
      activeViewId.value = mergedViews[0]?.id ?? activeViewId.value;
    } else {
      views.value = imported.views;
      activeViewId.value = imported.activeViewId;
    }

    applyLayout(activeView.value?.layout ?? null);
    refreshOpenPanels();
    await persistState().catch(() => undefined);
  }

  return {
    views,
    activeViewId,
    activeView,
    openPanelIds,
    onReady,
    switchView,
    createView,
    renameView,
    deleteView,
    togglePanel,
    resetCurrentView,
    exportViewsJson,
    importViewsJson,
  };
}
