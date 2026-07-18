<script setup lang="ts">
import type { DockviewPanelApi } from "dockview-vue";
import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useCollapsibleOpen } from "../../controllers/useCollapsibleOpen";
import { useConfirm } from "../../controllers/useConfirm";
import { useTemplateDetail } from "../../controllers/useTemplateDetail";
import type { TemplateKind, UpdateTemplateRequest } from "../../models/template";
import type { TemplatePanelParams } from "../../stores/workspaceLayout";
import { selectTemplate, workspace } from "../../stores/workspace";
import DeleteButton from "../molecules/DeleteButton.vue";
import TemplateMonacoEditor from "../molecules/TemplateMonacoEditor.vue";
import BaseDetailsLayout from "../organisms/BaseDetailsLayout.vue";
import TemplateDetailForm from "../organisms/TemplateDetailForm.vue";

interface DockviewComponentProps {
  params?: TemplatePanelParams;
  api?: DockviewPanelApi;
}

const dockview = defineProps<{ params?: DockviewComponentProps }>();
const { t } = useI18n();
const { confirm } = useConfirm();
const formRef = ref<{ submit: () => void } | null>(null);
const { open: configOpen } = useCollapsibleOpen("template-config");

const isBoundTab = computed(() => !!dockview.params?.params?.templateId);

const boundTemplateId = () =>
  isBoundTab.value
    ? (dockview.params?.params?.templateId ?? null)
    : workspace.selectedTemplateId;

const { template, loading, error, save, remove } = useTemplateDetail(boundTemplateId, {
  syncWorkspace: !isBoundTab.value,
});

const content = ref("");
const kind = ref<TemplateKind>("TEXT");

watch(
  template,
  (next) => {
    if (!next) return;
    content.value = next.content;
    kind.value = next.kind;
  },
  { immediate: true },
);

const meta = computed(() => {
  if (!template.value) return undefined;
  return t(`templates.kinds.${template.value.kind}`);
});

const empty = computed(() =>
  !boundTemplateId() ? t("template.selectTemplate") : undefined,
);

async function onSave(request: UpdateTemplateRequest) {
  await save(request);
}

function onSubmit() {
  formRef.value?.submit();
}

async function onDelete() {
  const id = boundTemplateId();
  if (!id) return;
  const confirmed = await confirm({
    message: t("templates.deleteConfirm", { id }),
    confirmLabel: t("common.delete"),
    confirmVariant: "danger",
  });
  if (!confirmed) return;
  await remove();
  if (isBoundTab.value) {
    dockview.params?.api?.close();
    return;
  }
  selectTemplate(null);
}
</script>

<template>
  <div class="panel-body template-panel">
    <template v-if="!boundTemplateId()">
      <p class="muted">{{ empty }}</p>
    </template>
    <template v-else-if="loading && !template">
      <p class="muted">{{ $t("common.loading") }}</p>
    </template>
    <template v-else-if="template">
      <section class="template-editor" :aria-label="$t('templates.content')">
        <TemplateMonacoEditor v-model="content" :kind="kind" min-height="0" />
        <p class="muted template-hint">{{ $t("templates.placeholdersHint") }}</p>
      </section>
      <div class="template-config" :class="{ 'is-collapsed': !configOpen }">
        <BaseDetailsLayout
          collapsible
          v-model:body-open="configOpen"
          :collapsible-title="$t('common.configuration')"
          :ready="true"
          :meta="meta"
          :error="error"
          @submit="onSubmit"
        >
          <TemplateDetailForm
            ref="formRef"
            v-model:content="content"
            v-model:kind="kind"
            :template="template"
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
.template-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-height: 0;
  overflow: hidden;
}

.template-editor {
  flex: 1 1 58%;
  min-height: 16rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.template-editor :deep(.monaco-host) {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
}

.template-hint {
  margin: 0;
  flex: 0 0 auto;
  font-size: 0.8rem;
}

.template-config {
  flex: 0 1 42%;
  min-height: 0;
  overflow: hidden;
  border-top: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}

.template-config.is-collapsed {
  flex: 0 0 auto;
}

.muted {
  color: var(--muted);
}
</style>
