<script setup lang="ts">
import { computed, markRaw, ref, shallowRef, watch } from "vue";
import { useI18n } from "vue-i18n";
import {
  JsonSchemaForm,
  type SchemaDocument,
} from "@jsonschema-editor/vue";
import { documentFromJSONWithExtensions } from "@jsonschema-editor/json-schema-extensions";
import { UiSchema } from "@jsonschema-editor/ui-schema/bridge";
import {
  EMPTY_PARAMETERS_SCHEMA,
  extractCreateParametersSchema,
  isUiSchemaObject,
} from "../../models/planSchemas";

const props = defineProps<{
  schemas: Record<string, unknown> | null | undefined;
  parametersUiSchema: Record<string, unknown> | null | undefined;
  modelValue: Record<string, unknown>;
  readonly?: boolean;
  /** Remount key (e.g. plan id) so the editor picks up schema changes cleanly. */
  formKey?: string;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: Record<string, unknown>];
}>();

const { locale } = useI18n();
/** Package ships de/en; other UI locales fall back to en. */
const jseLocale = computed(() => (locale.value === "de" ? "de" : "en"));

const formRef = ref<{ validate: () => boolean } | null>(null);
const schemaDoc = shallowRef<SchemaDocument>(
  markRaw(documentFromJSONWithExtensions(EMPTY_PARAMETERS_SCHEMA)),
);
const uiSchema = shallowRef(
  markRaw(UiSchema.generateForSchema(schemaDoc.value.root)),
);
const editorEpoch = ref(0);

const hasFields = computed(() => {
  const parameters = extractCreateParametersSchema(props.schemas);
  const properties = parameters.properties ?? {};
  return Object.keys(properties).length > 0;
});

const editorKey = computed(
  () => `${props.formKey ?? "plan"}-${editorEpoch.value}-${hasFields.value}`,
);

function loadSchema() {
  const parameters = extractCreateParametersSchema(props.schemas);
  const nextSchema = markRaw(documentFromJSONWithExtensions(parameters));
  schemaDoc.value = nextSchema;
  if (isUiSchemaObject(props.parametersUiSchema)) {
    uiSchema.value = markRaw(UiSchema.fromJSON(props.parametersUiSchema));
  } else {
    uiSchema.value = markRaw(UiSchema.generateForSchema(nextSchema.root));
  }
  editorEpoch.value += 1;

  // Apply JSON Schema defaults into the model when empty.
  if (!props.readonly) {
    const defaults: Record<string, unknown> = {};
    const properties = parameters.properties ?? {};
    for (const [key, def] of Object.entries(properties)) {
      if (
        def
        && typeof def === "object"
        && !Array.isArray(def)
        && "default" in def
        && props.modelValue[key] === undefined
      ) {
        defaults[key] = (def as { default: unknown }).default;
      }
    }
    if (Object.keys(defaults).length > 0) {
      emit("update:modelValue", { ...defaults, ...props.modelValue });
    }
  }
}

watch(
  () => [props.schemas, props.parametersUiSchema, props.formKey] as const,
  () => loadSchema(),
  { immediate: true, deep: true },
);

function onDataUpdate(value: Record<string, unknown>) {
  emit("update:modelValue", value);
}

function validate(): boolean {
  if (!hasFields.value) return true;
  return formRef.value?.validate() ?? true;
}

defineExpose({ validate, hasFields });
</script>

<template>
  <div v-if="hasFields" class="parameters-form">
    <JsonSchemaForm
      :key="editorKey"
      ref="formRef"
      :model-value="modelValue"
      :schema="schemaDoc"
      :ui-schema="uiSchema"
      :locale="jseLocale"
      :readonly="readonly"
      validation
      validation-mode="blur"
      @update:model-value="onDataUpdate"
    />
  </div>
  <p v-else class="muted">{{ $t("instances.noParameters") }}</p>
</template>

<style scoped>
.parameters-form {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0.65rem 0.75rem;
  background: var(--surface);
}
.muted {
  margin: 0.25rem 0 0.75rem;
  color: var(--muted);
  font-size: 0.85rem;
}
</style>
