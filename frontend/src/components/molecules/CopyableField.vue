<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import BaseInput from "../atoms/BaseInput.vue";
import FormField from "./FormField.vue";

const props = defineProps<{
  label: string;
  value: string;
  secret?: boolean;
}>();

const { t } = useI18n();
const copied = ref(false);
let copiedTimer: ReturnType<typeof setTimeout> | undefined;

async function copy() {
  try {
    await navigator.clipboard.writeText(props.value);
    copied.value = true;
    if (copiedTimer) clearTimeout(copiedTimer);
    copiedTimer = setTimeout(() => {
      copied.value = false;
    }, 1500);
  } catch {
    copied.value = false;
  }
}
</script>

<template>
  <FormField :label="label">
    <div class="copyable-field">
      <BaseInput
        :model-value="value"
        disabled
        :type="secret ? 'password' : 'text'"
        autocomplete="off"
      />
      <button type="button" class="copyable-field__btn" @click="copy">
        {{ copied ? t("platforms.copied") : t("platforms.copy") }}
      </button>
    </div>
  </FormField>
</template>

<style scoped>
.copyable-field {
  display: flex;
  gap: 0.45rem;
  align-items: stretch;
}

.copyable-field :deep(.base-input) {
  flex: 1;
  min-width: 0;
}

.copyable-field__btn {
  flex-shrink: 0;
  padding: 0.45rem 0.7rem;
  font: inherit;
  font-size: 0.85rem;
  cursor: pointer;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text);
  white-space: nowrap;
}

.copyable-field__btn:hover {
  border-color: var(--accent);
}
</style>
