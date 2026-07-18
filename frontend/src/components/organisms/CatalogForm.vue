<script setup lang="ts">
import { ref } from "vue";
import type { CreateCatalogRequest } from "../../models/catalogAdmin";
import BaseInput from "../atoms/BaseInput.vue";
import CreateButton from "../molecules/CreateButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";
import IdInput from "../molecules/IdInput.vue";

const emit = defineEmits<{ save: [request: CreateCatalogRequest] }>();

const id = ref("");
const name = ref("");
const description = ref("");

function submit() {
  emit("save", {
    id: id.value.trim(),
    name: name.value.trim(),
    description: description.value,
  });
  id.value = "";
  name.value = "";
  description.value = "";
}
</script>

<template>
  <form class="catalog-form" @submit.prevent="submit">
    <FormField :label="$t('common.id')">
      <IdInput
        v-model="id"
        required
        pattern="[a-z0-9-]+"
        :placeholder="$t('catalogs.idPlaceholder')"
      />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <p class="muted">{{ $t("catalogs.assignHint") }}</p>
    <CreateButton type="submit" :label="$t('common.create')" />
  </form>
</template>
