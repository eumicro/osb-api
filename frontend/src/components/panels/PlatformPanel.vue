<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useCatalogs } from "../../controllers/useCatalogs";
import { useConfirm } from "../../controllers/useConfirm";
import { usePlatformDetail } from "../../controllers/usePlatformDetail";
import type { UpdatePlatformClientRequest } from "../../models/platformClient";
import { applyPlatformSelection } from "../../stores/selectionSync";
import type { PlatformPanelParams } from "../../stores/workspaceLayout";
import {
  clearRevealedPlatformPassword,
  setRevealedPlatformPassword,
  workspace,
} from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import PlatformDetailForm from "../organisms/PlatformDetailForm.vue";

interface DockviewComponentProps {
  params?: PlatformPanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const { catalogs, load: loadCatalogs } = useCatalogs();
const formRef = ref<{ submit: () => void } | null>(null);

const isBoundTab = computed(() => !!dockview.params?.params?.platformId);

const boundPlatformId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.platformId ?? null)
    : workspace.selectedPlatformClientId;

const { platform, loading, error, save, remove } = usePlatformDetail(boundPlatformId, {
  syncWorkspace: !isBoundTab.value,
});

const revealedPassword = computed(() => {
  const handoff = workspace.revealedPlatformPassword;
  const id = boundPlatformId();
  if (!handoff || !id || handoff.platformId !== id) return null;
  return handoff.password;
});

const meta = computed(() => {
  if (!platform.value) return undefined;
  return `${platform.value.username} → ${platform.value.catalogId}`;
});

const empty = computed(() =>
  !boundPlatformId() ? t("platform.selectPlatform") : undefined,
);

watch(
  () => boundPlatformId(),
  (id, prev) => {
    if (id !== prev
      && workspace.revealedPlatformPassword
      && workspace.revealedPlatformPassword.platformId !== id) {
      clearRevealedPlatformPassword();
    }
  },
);

onMounted(() => {
  void loadCatalogs();
});

async function onSave(request: UpdatePlatformClientRequest) {
  const id = boundPlatformId();
  const ok = await save(request);
  if (ok && id && request.password) {
    setRevealedPlatformPassword(id, request.password);
  }
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundPlatformId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("platforms.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  clearRevealedPlatformPassword();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  applyPlatformSelection(null, catalogs.value);
}
</script>

<template>
  <div class="panel-body">
    <BaseDetailsLayout
      :ready="!!platform"
      :loading="loading && !platform"
      :empty="empty"
      :meta="meta"
      :error="error"
      @submit="onSubmit"
    >
      <PlatformDetailForm
        v-if="platform"
        ref="formRef"
        :platform="platform"
        :catalogs="catalogs"
        :revealed-password="revealedPassword"
        @save="onSave"
      />
      <template #actions>
        <DeleteButton :label="$t('common.delete')" @click="onDelete" />
      </template>
    </BaseDetailsLayout>
  </div>
</template>
