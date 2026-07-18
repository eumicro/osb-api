<script setup lang="ts">
import type { Me } from "../../models/me";
import type { WorkspaceView } from "../../models/workspaceView";
import type { AppLocale } from "../../i18n";
import type { UiTheme } from "../../theme";
import TopBar from "../organisms/TopBar.vue";

/** Template: Kopfleiste + frei gestaltbarer Workspace-Bereich (Dockview). */
defineProps<{
  me: Me | null;
  views: WorkspaceView[];
  activeViewId: string | null;
  openPanelIds: string[];
  locale: string;
  theme: string;
}>();

defineEmits<{
  switchView: [viewId: string];
  createView: [name: string, presetId: string];
  renameView: [name: string];
  deleteView: [];
  togglePanel: [panelId: string];
  resetCurrentView: [];
  exportViews: [];
  importViews: [raw: unknown, mode: "replace" | "merge"];
  changeLocale: [locale: AppLocale];
  changeTheme: [theme: UiTheme];
}>();
</script>

<template>
  <div class="workspace-layout">
    <TopBar
      :me="me"
      :views="views"
      :active-view-id="activeViewId"
      :open-panel-ids="openPanelIds"
      :locale="locale"
      :theme="theme"
      @switch-view="$emit('switchView', $event)"
      @create-view="(name, preset) => $emit('createView', name, preset)"
      @rename-view="$emit('renameView', $event)"
      @delete-view="$emit('deleteView')"
      @toggle-panel="$emit('togglePanel', $event)"
      @reset-current-view="$emit('resetCurrentView')"
      @export-views="$emit('exportViews')"
      @import-views="(raw, mode) => $emit('importViews', raw, mode)"
      @change-locale="$emit('changeLocale', $event)"
      @change-theme="$emit('changeTheme', $event)"
    />
    <div class="workspace-dock">
      <slot />
    </div>
  </div>
</template>
