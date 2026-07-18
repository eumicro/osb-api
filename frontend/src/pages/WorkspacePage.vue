<script setup lang="ts">
import { computed, onMounted } from "vue";
import { DockviewVue } from "dockview-vue";
import { useMe } from "../controllers/useMe";
import { useUiLayout } from "../controllers/useUiLayout";
import { useUiLocale } from "../controllers/useUiLocale";
import { useUiTheme } from "../controllers/useUiTheme";
import { dockviewThemeFor } from "../theme";
import WorkspaceLayout from "../components/templates/WorkspaceLayout.vue";

/**
 * Page: der Workspace. Mehrsprachiges Admin-UI mit Hell/Dunkel-Modus und
 * mehreren benannten Ansichten pro Benutzer.
 */
const { me, refresh: refreshMe } = useMe();
const { locale, setLocale } = useUiLocale();
const { theme, setTheme } = useUiTheme();
const {
  views,
  activeViewId,
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
} = useUiLayout();

const dockviewTheme = computed(() => dockviewThemeFor(theme.value));

onMounted(() => {
  void refreshMe();
});
</script>

<template>
  <WorkspaceLayout
    :me="me"
    :views="views"
    :active-view-id="activeViewId"
    :open-panel-ids="openPanelIds"
    :locale="String(locale)"
    :theme="theme"
    @switch-view="switchView"
    @create-view="createView"
    @rename-view="renameView"
    @delete-view="deleteView"
    @toggle-panel="togglePanel"
    @reset-current-view="resetCurrentView"
    @export-views="exportViewsJson"
    @import-views="(raw, mode) => importViewsJson(raw, mode)"
    @change-locale="setLocale"
    @change-theme="setTheme"
  >
    <!-- Panels werden ueber ihre global registrierten Komponentennamen erzeugt (siehe main.ts). -->
    <DockviewVue
      :theme="dockviewTheme"
      class="workspace-dockview"
      @ready="onReady"
    />
  </WorkspaceLayout>
</template>
