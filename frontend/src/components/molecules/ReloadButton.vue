<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import BaseButton from "../atoms/BaseButton.vue";

const props = defineProps<{ loading: boolean }>();
defineEmits<{ click: [] }>();

const { t } = useI18n();
const label = computed(() => (props.loading ? t("common.loading") : t("common.reload")));
</script>

<template>
  <BaseButton
    icon="refresh"
    :label="label"
    variant="secondary"
    :disabled="loading"
    @click="$emit('click')"
  />
</template>
