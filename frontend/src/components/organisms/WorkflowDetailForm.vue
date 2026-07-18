<script setup lang="ts">
import { ref, watch } from "vue";
import {
  WORKFLOW_KINDS,
  type UpdateWorkflowRequest,
  type WorkflowDefinition,
  type WorkflowKind,
} from "../../models/workflow";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const props = defineProps<{ workflow: WorkflowDefinition }>();
const emit = defineEmits<{ save: [request: UpdateWorkflowRequest] }>();

const name = ref(props.workflow.name);
const description = ref(props.workflow.description);
const kind = ref<WorkflowKind>(props.workflow.kind);
const n8nWebhookPath = ref(props.workflow.n8nWebhookPath);
const n8nWorkflowId = ref(props.workflow.n8nWorkflowId ?? "");
const enabled = ref(props.workflow.enabled);

watch(
  () => props.workflow,
  (workflow) => {
    name.value = workflow.name;
    description.value = workflow.description;
    kind.value = workflow.kind;
    n8nWebhookPath.value = workflow.n8nWebhookPath;
    n8nWorkflowId.value = workflow.n8nWorkflowId ?? "";
    enabled.value = workflow.enabled;
  },
);

function submit() {
  // Clients/templates are chosen in n8n nodes — keep existing DB links on save.
  emit("save", {
    name: name.value.trim(),
    description: description.value.trim(),
    kind: kind.value,
    n8nWebhookPath: n8nWebhookPath.value.trim(),
    n8nWorkflowId: n8nWorkflowId.value.trim(),
    enabled: enabled.value,
    clients: [...props.workflow.clients],
    httpClientIds: [...(props.workflow.httpClientIds ?? [])],
    kubernetesClientIds: [...(props.workflow.kubernetesClientIds ?? [])],
    gitClientIds: [...(props.workflow.gitClientIds ?? [])],
  });
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('common.id')">
      <BaseInput :model-value="workflow.id" disabled />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <FormField :label="$t('workflows.kind')">
      <BaseSelect v-model="kind" required>
        <option v-for="entry in WORKFLOW_KINDS" :key="entry" :value="entry">
          {{ $t(`workflows.kinds.${entry}`) }}
        </option>
      </BaseSelect>
    </FormField>
    <FormField :label="$t('workflows.webhook')">
      <BaseInput v-model="n8nWebhookPath" required />
    </FormField>
    <FormField :label="$t('workflows.n8nWorkflowId')">
      <BaseInput v-model="n8nWorkflowId" :placeholder="$t('workflows.n8nWorkflowIdPlaceholder')" />
    </FormField>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("workflows.enabled") }}
    </label>
    <p class="muted">{{ $t("workflows.n8nClientsHint") }}</p>
  </div>
</template>

<style scoped>
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.45rem;
  font-size: 0.9rem;
}
.muted {
  margin: 0.5rem 0 0;
  color: var(--muted);
  font-size: 0.85rem;
}
</style>
