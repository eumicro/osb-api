<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useConfirm } from "../../controllers/useConfirm";
import type { WorkspaceView } from "../../models/workspaceView";
import { parseExportDocument } from "../../models/workspaceViewExport";
import { PANELS, VIEW_PRESETS } from "../../workspace/panels";
import BaseButton from "../atoms/BaseButton.vue";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import DialogFormActions from "../molecules/DialogFormActions.vue";
import AppDialog from "./AppDialog.vue";

/**
 * Organism: Verwaltung und Wechsel zwischen mehreren benannten Workspace-Ansichten.
 */
const props = defineProps<{
  views: WorkspaceView[];
  activeViewId: string | null;
}>();

const emit = defineEmits<{
  switchView: [viewId: string];
  createView: [name: string, presetId: string];
  renameView: [name: string];
  deleteView: [];
  resetCurrentView: [];
  exportViews: [];
  importViews: [raw: unknown, mode: "replace" | "merge"];
}>();

const { t } = useI18n();
const { confirm } = useConfirm();

const menuOpen = ref(false);
const createDialogOpen = ref(false);
const renameDialogOpen = ref(false);
const newViewName = ref("");
const newViewPreset = ref("current");
const renameName = ref("");
const importInput = ref<HTMLInputElement | null>(null);
const pendingImportMode = ref<"replace" | "merge">("replace");

const activeViewName = computed(
  () => props.views.find((view) => view.id === props.activeViewId)?.name ?? "…",
);
const viewMenuLabel = computed(() => `${t("views.label")}: ${activeViewName.value}`);

const presetOptions = computed(() =>
  VIEW_PRESETS.map((preset) => {
    if (preset.id === "current") {
      return { value: preset.id, label: t("views.presets.current") };
    }
    if (preset.id === "default") {
      return { value: preset.id, label: t("views.presets.default") };
    }
    const panel = PANELS.find((entry) => entry.id === preset.id);
    return {
      value: preset.id,
      label: t("views.presets.only", { panel: panel ? t(panel.titleKey) : preset.id }),
    };
  }),
);

function closeMenu() {
  menuOpen.value = false;
}

function onSwitch(viewId: string) {
  emit("switchView", viewId);
  closeMenu();
}

function openCreateDialog() {
  closeMenu();
  createDialogOpen.value = true;
}

function openRenameDialog() {
  renameName.value = activeViewName.value === "…" ? "" : activeViewName.value;
  closeMenu();
  renameDialogOpen.value = true;
}

function onCreate() {
  if (!newViewName.value.trim()) return;
  emit("createView", newViewName.value.trim(), newViewPreset.value);
  newViewName.value = "";
  newViewPreset.value = "current";
  createDialogOpen.value = false;
}

function onRename() {
  if (!renameName.value.trim()) return;
  emit("renameView", renameName.value.trim());
  renameDialogOpen.value = false;
}

async function onDelete() {
  closeMenu();
  const confirmed = await confirm({
    message: t("views.deleteConfirm"),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (confirmed) emit("deleteView");
}

async function onReset() {
  closeMenu();
  const confirmed = await confirm({
    message: t("views.resetConfirm"),
    confirmLabel: t("dialog.confirm"),
  });
  if (confirmed) emit("resetCurrentView");
}

function onExport() {
  closeMenu();
  emit("exportViews");
}

function onImportClick(mode: "replace" | "merge") {
  closeMenu();
  pendingImportMode.value = mode;
  importInput.value?.click();
}

async function onImportFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;

  try {
    const text = await file.text();
    const raw = JSON.parse(text) as unknown;
    parseExportDocument(raw);
    if (pendingImportMode.value === "replace") {
      const confirmed = await confirm({
        message: t("views.importReplaceConfirm"),
        confirmLabel: t("views.importReplace"),
        confirmVariant: "danger",
      });
      if (!confirmed) return;
    }
    emit("importViews", raw, pendingImportMode.value);
  } catch {
    await confirm({
      message: t("views.importInvalid"),
      confirmLabel: t("dialog.close"),
    });
  }
}
</script>

<template>
  <div class="menu view-menu">
    <BaseButton
      icon="layout"
      :label="viewMenuLabel"
      variant="secondary"
      @click="menuOpen = !menuOpen"
    />

    <div v-if="menuOpen" class="dropdown view-dropdown" @mouseleave="closeMenu">
      <section class="dropdown-section">
        <div class="dropdown-heading">{{ $t("views.views") }}</div>
        <button
          v-for="view in views"
          :key="view.id"
          class="dropdown-item as-text view-item"
          :class="{ active: view.id === activeViewId }"
          @click="onSwitch(view.id)"
        >
          <span class="view-marker">{{ view.id === activeViewId ? "●" : "○" }}</span>
          {{ view.name }}
        </button>
      </section>

      <hr />

      <section class="dropdown-section">
        <button class="dropdown-item as-text" @click="openCreateDialog">
          {{ $t("views.new") }}
        </button>
        <button class="dropdown-item as-text" @click="openRenameDialog">
          {{ $t("views.rename") }}
        </button>
        <button
          v-if="views.length > 1"
          class="dropdown-item as-text danger-text"
          @click="onDelete"
        >
          {{ $t("views.delete") }}
        </button>
      </section>

      <hr />

      <section class="dropdown-section">
        <button class="dropdown-item as-text" @click="onExport">
          {{ $t("views.exportJson") }}
        </button>
        <button class="dropdown-item as-text" @click="onImportClick('replace')">
          {{ $t("views.importJsonReplace") }}
        </button>
        <button class="dropdown-item as-text" @click="onImportClick('merge')">
          {{ $t("views.importJsonMerge") }}
        </button>
      </section>

      <hr />

      <button class="dropdown-item as-text" @click="onReset">
        {{ $t("views.reset") }}
      </button>
    </div>

    <input
      ref="importInput"
      type="file"
      accept="application/json,.json"
      class="import-input"
      @change="onImportFile"
    />

    <AppDialog v-model:open="createDialogOpen" :title="$t('views.newTitle')">
      <BaseInput v-model="newViewName" :placeholder="$t('views.newNamePlaceholder')" />
      <BaseSelect v-model="newViewPreset" style="margin-top: 0.75rem; width: 100%">
        <option v-for="preset in presetOptions" :key="preset.value" :value="preset.value">
          {{ preset.label }}
        </option>
      </BaseSelect>
      <template #footer>
        <DialogFormActions
          :cancel-label="$t('common.cancel')"
          :submit-label="$t('common.create')"
          submit-icon="plus"
          @cancel="createDialogOpen = false"
          @submit="onCreate"
        />
      </template>
    </AppDialog>

    <AppDialog v-model:open="renameDialogOpen" :title="$t('views.renameTitle')">
      <BaseInput v-model="renameName" :placeholder="$t('views.renamePlaceholder')" />
      <template #footer>
        <DialogFormActions
          :cancel-label="$t('common.cancel')"
          :submit-label="$t('common.save')"
          submit-icon="save"
          @cancel="renameDialogOpen = false"
          @submit="onRename"
        />
      </template>
    </AppDialog>
  </div>
</template>

<style scoped>
.import-input {
  display: none;
}
</style>
