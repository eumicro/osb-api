<script setup lang="ts">
import BaseInput from "../atoms/BaseInput.vue";
import GenerateUuidButton from "./GenerateUuidButton.vue";

/** Molecule: ID text field with generate-UUID icon button. */
defineOptions({ inheritAttrs: false });

const model = defineModel<string>({ required: true });

function generateUuid() {
  model.value = crypto.randomUUID();
}
</script>

<template>
  <div class="id-input">
    <BaseInput v-model="model" v-bind="$attrs" />
    <GenerateUuidButton @click="generateUuid" />
  </div>
</template>

<style scoped>
.id-input {
  display: flex;
  align-items: stretch;
  gap: 0.4rem;
}

.id-input :deep(.base-input) {
  flex: 1;
  min-width: 0;
}

.id-input :deep(.base-button) {
  flex-shrink: 0;
  align-self: center;
}
</style>
