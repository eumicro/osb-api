<script setup lang="ts">
import { useI18n } from "vue-i18n";
import BaseTextarea from "../atoms/BaseTextarea.vue";
import FormField from "./FormField.vue";

/** Molecule: labeled multi-line description field. */
defineOptions({ inheritAttrs: false });

const model = defineModel<string>({ required: true });
const { t } = useI18n();
</script>

<template>
  <FormField :label="t('common.description')">
    <BaseTextarea v-model="model" v-bind="$attrs" />
  </FormField>
</template>
