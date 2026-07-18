<script setup lang="ts">
import { computed, markRaw, shallowRef, watch } from "vue";
import { useI18n } from "vue-i18n";
import {
  JsonSchemaFormEditor,
  type SchemaDocument,
} from "@jsonschema-editor/vue";
import { documentFromJSONWithExtensions } from "@jsonschema-editor/json-schema-extensions";
import type { JsonSchemaObject } from "@jsonschema-editor/json-schema";
import { UiSchema } from "@jsonschema-editor/ui-schema/bridge";
import type { UiSchemaObject } from "@jsonschema-editor/ui-schema";
import {
  EMPTY_PARAMETERS_SCHEMA,
  extractCreateParametersSchema,
  isUiSchemaObject,
  withCreateParametersSchema,
} from "../../models/planSchemas";

const props = defineProps<{
  schemas: Record<string, unknown> | null | undefined;
  parametersUiSchema: Record<string, unknown> | null | undefined;
}>();

const emit = defineEmits<{
  "update:schemas": [value: Record<string, unknown>];
  "update:parametersUiSchema": [value: Record<string, unknown>];
}>();

const { locale } = useI18n();
/** Package ships de/en; other UI locales fall back to en. Do not pass vue-i18n translate — missing jse.* keys would block built-in messages. */
const jseLocale = computed(() => (locale.value === "de" ? "de" : "en"));

const previewData = shallowRef<Record<string, unknown>>({});
const schemaDoc = shallowRef<SchemaDocument>(
  markRaw(documentFromJSONWithExtensions(EMPTY_PARAMETERS_SCHEMA)),
);
const uiSchema = shallowRef<UiSchema>(
  markRaw(UiSchema.generateForSchema(schemaDoc.value.root)),
);

function loadFromProps() {
  const parameters = extractCreateParametersSchema(props.schemas);
  const nextSchema = markRaw(documentFromJSONWithExtensions(parameters));
  schemaDoc.value = nextSchema;
  if (isUiSchemaObject(props.parametersUiSchema)) {
    uiSchema.value = markRaw(UiSchema.fromJSON(props.parametersUiSchema));
  } else {
    uiSchema.value = markRaw(UiSchema.generateForSchema(nextSchema.root));
  }
  previewData.value = {};
}

watch(
  () => [props.schemas, props.parametersUiSchema] as const,
  () => loadFromProps(),
  { immediate: true, deep: true },
);

function onSchemaUpdate(next: SchemaDocument) {
  schemaDoc.value = markRaw(next);
  const json = next.toJSON() as JsonSchemaObject;
  emit("update:schemas", withCreateParametersSchema(props.schemas, json) as Record<string, unknown>);
}

function onUiSchemaUpdate(next: UiSchema) {
  uiSchema.value = markRaw(next);
  emit("update:parametersUiSchema", next.toJSON() as UiSchemaObject as Record<string, unknown>);
}

function onPreviewUpdate(value: Record<string, unknown>) {
  previewData.value = value;
}
</script>

<template>
  <div class="schema-editor">
    <JsonSchemaFormEditor
      :model-value="previewData"
      :schema="schemaDoc"
      :ui-schema="uiSchema"
      :locale="jseLocale"
      @update:model-value="onPreviewUpdate"
      @update:schema="onSchemaUpdate"
      @update:ui-schema="onUiSchemaUpdate"
    />
  </div>
</template>

<style scoped>
.schema-editor {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0.5rem;
  background: var(--surface);
  min-height: 12rem;
}
</style>
