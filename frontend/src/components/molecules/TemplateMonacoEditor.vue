<script setup lang="ts">
import { VueMonacoEditor } from "@guolao/vue-monaco-editor";
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import type { TemplateKind } from "../../models/template";
import { detectSystemTheme, type UiTheme } from "../../theme";

const props = withDefaults(
  defineProps<{
    modelValue: string;
    kind?: TemplateKind;
    readonly?: boolean;
    minHeight?: string;
  }>(),
  {
    kind: "TEXT",
    readonly: false,
    minHeight: "280px",
  },
);

const emit = defineEmits<{ "update:modelValue": [value: string] }>();

const editorTheme = ref<"vs" | "vs-dark">(
  resolveTheme(detectSystemTheme()) === "dark" ? "vs-dark" : "vs",
);

const language = computed(() => {
  switch (props.kind) {
    case "KUBERNETES_RESOURCE":
      return "yaml";
    case "HTTP_REQUEST":
      return "json";
    case "GIT_COMMAND":
      return "shell";
    default:
      return "plaintext";
  }
});

const options = computed(() => ({
  automaticLayout: true,
  minimap: { enabled: true },
  fontSize: 13,
  fontFamily: "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace",
  wordWrap: "on" as const,
  scrollBeyondLastLine: false,
  tabSize: 2,
  renderWhitespace: "selection" as const,
  readOnly: props.readonly,
  padding: { top: 8, bottom: 8 },
}));

function resolveTheme(theme: UiTheme): UiTheme {
  return theme;
}

function syncThemeFromDom() {
  const attr = document.documentElement.getAttribute("data-theme");
  editorTheme.value = attr === "dark" ? "vs-dark" : "vs";
}

let observer: MutationObserver | null = null;

onMounted(() => {
  syncThemeFromDom();
  observer = new MutationObserver(syncThemeFromDom);
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["data-theme"],
  });
});

onUnmounted(() => {
  observer?.disconnect();
});

watch(
  () => props.kind,
  () => {
    /* language bound via :language */
  },
);

function onUpdate(value: string) {
  emit("update:modelValue", value);
}
</script>

<template>
  <div class="monaco-host" :style="{ minHeight }">
    <VueMonacoEditor
      :value="modelValue"
      :language="language"
      :theme="editorTheme"
      :options="options"
      class="monaco"
      @update:value="onUpdate"
    />
  </div>
</template>

<style scoped>
.monaco-host {
  width: 100%;
  height: 100%;
  min-height: v-bind(minHeight);
  border: 1px solid var(--border);
  border-radius: 4px;
  overflow: hidden;
  background: var(--surface);
}
.monaco {
  width: 100%;
  height: 100%;
  min-height: inherit;
}
</style>
