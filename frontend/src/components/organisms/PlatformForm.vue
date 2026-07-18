<script setup lang="ts">
import { ref, watch } from "vue";
import type { AdminCatalog } from "../../models/catalog";
import type { CreatePlatformClientRequest } from "../../models/platformClient";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import CreateButton from "../molecules/CreateButton.vue";
import FormField from "../molecules/FormField.vue";

const props = defineProps<{ catalogs: AdminCatalog[] }>();
const emit = defineEmits<{ save: [request: CreatePlatformClientRequest] }>();

const displayName = ref("");
const username = ref("");
const catalogId = ref("");
const enabled = ref(true);

watch(
  () => props.catalogs,
  (catalogs) => {
    if (!catalogId.value && catalogs.length > 0) catalogId.value = catalogs[0].id;
  },
  { immediate: true },
);

function submit() {
  emit("save", {
    displayName: displayName.value.trim(),
    username: username.value.trim(),
    catalogId: catalogId.value,
    enabled: enabled.value,
  });
  displayName.value = "";
  username.value = "";
  enabled.value = true;
}
</script>

<template>
  <form @submit.prevent="submit">
    <FormField :label="$t('common.name')">
      <BaseInput v-model="displayName" required />
    </FormField>
    <FormField :label="$t('platforms.username')">
      <BaseInput v-model="username" required />
    </FormField>
    <FormField :label="$t('platforms.catalog')">
      <BaseSelect v-model="catalogId" required>
        <option v-for="catalog in catalogs" :key="catalog.id" :value="catalog.id">
          {{ catalog.name }} ({{ catalog.id }})
        </option>
      </BaseSelect>
    </FormField>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("platforms.enabled") }}
    </label>
    <CreateButton type="submit" :label="$t('common.create')" />
  </form>
</template>

<style scoped>
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.85rem;
  font-size: 0.9rem;
}
</style>
