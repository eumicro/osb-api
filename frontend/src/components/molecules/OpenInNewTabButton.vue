<script setup lang="ts">
import { useI18n } from "vue-i18n";
import BaseButton from "../atoms/BaseButton.vue";

/** Molecule: icon button to open an item in a new workspace tab. */
defineEmits<{ click: [] }>();

const { t } = useI18n();
</script>

<template>
  <BaseButton
    icon="tab"
    :label="t('common.openTab')"
    variant="secondary"
    type="button"
    @click="$emit('click')"
  />
</template>
