<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useCollapsibleOpen } from "../../controllers/useCollapsibleOpen";
import { useConfirm } from "../../controllers/useConfirm";
import { useWorkflowDetail } from "../../controllers/useWorkflowDetail";
import type { UpdateWorkflowRequest } from "../../models/workflow";
import {
  WORKFLOW_SELECTED_EVENT,
  notifyWorkflowPanelRefresh,
} from "../../stores/catalogSelection";
import type { WorkflowPanelParams } from "../../stores/workspaceLayout";
import { selectWorkflow, workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import OpenInNewTabLink from "../molecules/OpenInNewTabLink.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import N8nEmbed from "../organisms/N8nEmbed.vue";
import WorkflowDetailForm from "../organisms/WorkflowDetailForm.vue";

interface DockviewComponentProps {
  params?: WorkflowPanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const formRef = ref<{ submit: () => void } | null>(null);
const { open: configOpen } = useCollapsibleOpen("workflow-config");

const isBoundTab = computed(() => !!dockview.params?.params?.workflowId);

const boundWorkflowId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.workflowId ?? null)
    : workspace.selectedWorkflowId;

const { workflow, loading, error, load, save, remove } = useWorkflowDetail(boundWorkflowId, {
  syncWorkspace: !isBoundTab.value,
});

/**
 * Same-origin BFF bridge propagates the alice Keycloak session into n8n
 * (iframe cannot rely on third-party SSO cookies on :8180).
 */
const n8nEmbedSrc = computed(() => {
  const editorId = workflow.value?.n8nWorkflowId?.trim();
  const returnTo = editorId ? `/workflow/${editorId}` : "/";
  return `/bff/n8n-embed?returnTo=${encodeURIComponent(returnTo)}`;
});

const n8nOpenHref = computed(() => n8nEmbedSrc.value);

const meta = computed(() => {
  if (!workflow.value) return undefined;
  const n8n = workflow.value.n8nWorkflowId
    ? ` · n8n:${workflow.value.n8nWorkflowId}`
    : "";
  return `${workflow.value.kind} · ${workflow.value.n8nWebhookPath}${n8n}`;
});

const empty = computed(() =>
  !boundWorkflowId() ? t("workflow.selectWorkflow") : undefined,
);

onMounted(() => {
  window.addEventListener(WORKFLOW_SELECTED_EVENT, onExternalSelect);
});
onUnmounted(() => {
  window.removeEventListener(WORKFLOW_SELECTED_EVENT, onExternalSelect);
});

async function onSave(request: UpdateWorkflowRequest) {
  await save(request);
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundWorkflowId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("workflows.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  selectWorkflow(null);
  notifyWorkflowPanelRefresh(null);
}

function onExternalSelect() {
  if (isBoundTab.value) return;
  void load();
}
</script>

<template>
  <div class="panel-body workflow-panel">
    <template v-if="!boundWorkflowId()">
      <p class="muted">{{ empty }}</p>
    </template>
    <template v-else-if="loading && !workflow">
      <p class="muted">{{ $t("common.loading") }}</p>
    </template>
    <template v-else-if="workflow">
      <section class="workflow-n8n" aria-label="n8n">
        <div class="n8n-toolbar">
          <p v-if="!workflow.n8nWorkflowId" class="muted n8n-hint">
            {{ $t("workflows.n8nWorkflowIdMissing") }}
          </p>
          <span v-else />
          <OpenInNewTabLink :href="n8nOpenHref" :label="$t('workflows.openN8n')" />
        </div>
        <N8nEmbed :key="n8nEmbedSrc" :src="n8nEmbedSrc" />
      </section>
      <div class="workflow-config" :class="{ 'is-collapsed': !configOpen }">
        <BaseDetailsLayout
          collapsible
          v-model:body-open="configOpen"
          :collapsible-title="$t('common.configuration')"
          :ready="true"
          :meta="meta"
          :error="error"
          @submit="onSubmit"
        >
          <WorkflowDetailForm
            ref="formRef"
            :workflow="workflow"
            @save="onSave"
          />
          <template #actions>
            <DeleteButton :label="$t('common.delete')" @click="onDelete" />
          </template>
        </BaseDetailsLayout>
      </div>
    </template>
    <p v-else class="muted">{{ error || $t("common.error") }}</p>
  </div>
</template>

<style scoped>
.workflow-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-height: 0;
  overflow: hidden;
}

.workflow-n8n {
  flex: 1 1 58%;
  min-height: 16rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.workflow-config {
  flex: 0 1 42%;
  min-height: 0;
  overflow: hidden;
  border-top: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}

.workflow-config.is-collapsed {
  flex: 0 0 auto;
}

.n8n-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-height: 2.15rem;
}

.n8n-hint {
  margin: 0;
  font-size: 0.8rem;
}
</style>
