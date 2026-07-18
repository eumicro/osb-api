<script setup lang="ts">
import { ref, watch } from "vue";
import {
  TEMPLATE_KINDS,
  type Template,
  type TemplateKind,
  type UpdateTemplateRequest,
} from "../../models/template";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const props = defineProps<{ template: Template }>();
const content = defineModel<string>("content", { required: true });
const kind = defineModel<TemplateKind>("kind", { required: true });
const emit = defineEmits<{ save: [request: UpdateTemplateRequest] }>();

const name = ref(props.template.name);
const description = ref(props.template.description);
const enabled = ref(props.template.enabled);

watch(
  () => props.template,
  (template) => {
    name.value = template.name;
    description.value = template.description;
    enabled.value = template.enabled;
  },
);

function submit() {
  emit("save", {
    name: name.value.trim(),
    description: description.value.trim(),
    kind: kind.value,
    content: content.value,
    enabled: enabled.value,
  });
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('common.id')">
      <BaseInput :model-value="template.id" disabled />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <FormField :label="$t('templates.kind')">
      <BaseSelect v-model="kind" required>
        <option v-for="entry in TEMPLATE_KINDS" :key="entry" :value="entry">
          {{ $t(`templates.kinds.${entry}`) }}
        </option>
      </BaseSelect>
    </FormField>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("templates.enabled") }}
    </label>
  </div>
</template>

<style scoped>
.checkbox-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 0.75rem;
}
</style>
