<script setup lang="ts">
import { ref } from "vue";
import {
  TEMPLATE_KINDS,
  type CreateTemplateRequest,
  type TemplateKind,
} from "../../models/template";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import CreateButton from "../molecules/CreateButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";
import TemplateMonacoEditor from "../molecules/TemplateMonacoEditor.vue";

const emit = defineEmits<{ save: [request: CreateTemplateRequest] }>();

const name = ref("");
const description = ref("");
const kind = ref<TemplateKind>("KUBERNETES_RESOURCE");
const content = ref("apiVersion: v1\nkind: ConfigMap\nmetadata:\n  name: ${instanceId}\n");
const enabled = ref(true);

function submit() {
  emit("save", {
    name: name.value.trim(),
    description: description.value.trim(),
    kind: kind.value,
    content: content.value,
    enabled: enabled.value,
  });
  name.value = "";
  description.value = "";
  kind.value = "KUBERNETES_RESOURCE";
  content.value = "apiVersion: v1\nkind: ConfigMap\nmetadata:\n  name: ${instanceId}\n";
  enabled.value = true;
}
</script>

<template>
  <form class="template-create" @submit.prevent="submit">
    <section class="template-create__editor" :aria-label="$t('templates.content')">
      <TemplateMonacoEditor v-model="content" :kind="kind" min-height="280px" />
      <p class="muted">{{ $t("templates.placeholdersHint") }}</p>
    </section>
    <div class="template-create__fields">
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
      <CreateButton type="submit" :label="$t('common.create')" />
    </div>
  </form>
</template>

<style scoped>
.template-create {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-height: 0;
}

.template-create__editor {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.template-create__fields {
  border-top: 1px solid var(--border);
  padding-top: 0.75rem;
}

.muted {
  margin: 0;
  color: var(--muted);
  font-size: 0.85rem;
}
.checkbox-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 0.75rem;
}
</style>
