<script setup lang="ts">
import BaseSelect from "../atoms/BaseSelect.vue";
import FormField from "./FormField.vue";

const model = defineModel<string>({ required: true });

defineProps<{
  options: readonly string[];
  /** i18n key prefix, e.g. httpClients.authTypes */
  labelKey: string;
  optionKeyPrefix: string;
}>();
</script>

<template>
  <FormField :label="$t(labelKey)">
    <BaseSelect v-model="model" required>
      <option v-for="entry in options" :key="entry" :value="entry">
        {{ $t(`${optionKeyPrefix}.${entry}`) }}
      </option>
    </BaseSelect>
  </FormField>
</template>
