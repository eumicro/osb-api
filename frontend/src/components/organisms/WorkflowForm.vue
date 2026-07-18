<script setup lang="ts">
import { ref, watch } from "vue";
import {
  WORKFLOW_KIND_DEFAULTS,
  WORKFLOW_KINDS,
  type CreateWorkflowRequest,
  type WorkflowKind,
} from "../../models/workflow";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import CreateButton from "../molecules/CreateButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const emit = defineEmits<{ save: [request: CreateWorkflowRequest] }>();

const name = ref("");
const description = ref("");
const kind = ref<WorkflowKind>("PROVISION");
const n8nWebhookPath = ref("/webhook/osb-provision");
const n8nWorkflowId = ref("");
const enabled = ref(true);

watch(kind, (next) => {
  const defaults = WORKFLOW_KIND_DEFAULTS[next];
  n8nWebhookPath.value = defaults.webhookPath;
  if (!n8nWorkflowId.value.trim() || n8nWorkflowId.value.startsWith("osbWf")) {
    n8nWorkflowId.value = defaults.n8nWorkflowId;
  }
});

function submit() {
  emit("save", {
    name: name.value.trim(),
    description: description.value.trim(),
    kind: kind.value,
    n8nWebhookPath: n8nWebhookPath.value.trim(),
    n8nWorkflowId: n8nWorkflowId.value.trim(),
    enabled: enabled.value,
    clients: [],
    httpClientIds: [],
    kubernetesClientIds: [],
    gitClientIds: [],
  });
  name.value = "";
  description.value = "";
  kind.value = "PROVISION";
  n8nWebhookPath.value = WORKFLOW_KIND_DEFAULTS.PROVISION.webhookPath;
  n8nWorkflowId.value = WORKFLOW_KIND_DEFAULTS.PROVISION.n8nWorkflowId;
  enabled.value = true;
}
</script>

<template>
  <form @submit.prevent="submit">
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
      <BaseInput v-model="n8nWebhookPath" required placeholder="/webhook/..." />
    </FormField>
    <FormField :label="$t('workflows.n8nWorkflowId')">
      <BaseInput v-model="n8nWorkflowId" :placeholder="$t('workflows.n8nWorkflowIdPlaceholder')" />
    </FormField>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("workflows.enabled") }}
    </label>
    <p class="muted">{{ $t("workflows.n8nClientsHint") }}</p>
    <CreateButton type="submit" :label="$t('common.create')" />
  </form>
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
  margin: 0 0 0.85rem;
  color: var(--muted);
  font-size: 0.85rem;
}
</style>
