<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { AppLocale } from "../../i18n";
import type { Me } from "../../models/me";
import type { WorkspaceView } from "../../models/workspaceView";
import type { UiTheme } from "../../theme";
import BaseButton from "../atoms/BaseButton.vue";
import Icon from "../atoms/Icon.vue";
import LocaleSwitcher from "../molecules/LocaleSwitcher.vue";
import PanelSwitcher from "../molecules/PanelSwitcher.vue";
import ThemeSwitcher from "../molecules/ThemeSwitcher.vue";
import ViewSwitcher from "./ViewSwitcher.vue";

/** Organism: Kopfleiste mit Ansichtsverwaltung, UI-Sprache, Theme und Benutzerinfo. */
const props = defineProps<{
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

const { t } = useI18n();

const userLabel = computed(() => {
  if (!props.me) return "";
  const roles = props.me.roles.length ? ` (${props.me.roles.join(", ")})` : "";
  return `${props.me.name}${roles}`;
});

function logout() {
  window.location.href = "/bff/logout";
}
</script>

<template>
  <header class="topbar">
    <div class="brand">{{ $t("app.title") }}</div>

    <div class="topbar-menus">
      <ViewSwitcher
        :views="views"
        :active-view-id="activeViewId"
        @switch-view="$emit('switchView', $event)"
        @create-view="(name, preset) => $emit('createView', name, preset)"
        @rename-view="$emit('renameView', $event)"
        @delete-view="$emit('deleteView')"
        @reset-current-view="$emit('resetCurrentView')"
        @export-views="$emit('exportViews')"
        @import-views="(raw, mode) => $emit('importViews', raw, mode)"
      />
      <PanelSwitcher
        :open-panel-ids="openPanelIds"
        @toggle-panel="$emit('togglePanel', $event)"
      />
    </div>

    <div class="spacer"></div>

    <div class="topbar-actions">
      <ThemeSwitcher :theme="theme" @change="$emit('changeTheme', $event)" />
      <LocaleSwitcher :locale="locale" @change="$emit('changeLocale', $event)" />

      <template v-if="me">
        <span
          class="user-chip"
          :title="userLabel"
          :aria-label="userLabel"
          role="img"
        >
          <Icon name="user" />
        </span>
        <BaseButton
          icon="logOut"
          :label="t('app.logout')"
          variant="secondary"
          @click="logout"
        />
      </template>
    </div>
  </header>
</template>

<style scoped>
.topbar-menus,
.topbar-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.15rem;
  height: 2.15rem;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text);
}
</style>
