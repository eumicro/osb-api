<script setup lang="ts">
import { ref } from "vue";
import type { CreateOfferingRequest } from "../../models/catalogAdmin";
import BaseInput from "../atoms/BaseInput.vue";
import CreateButton from "../molecules/CreateButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";
import IdInput from "../molecules/IdInput.vue";

const emit = defineEmits<{ save: [request: CreateOfferingRequest] }>();

const id = ref("");
const name = ref("");
const description = ref("");
const bindable = ref(true);
const planId = ref("");
const planName = ref("");
const planDescription = ref("");
const planFree = ref(true);
const planBindable = ref(true);

function submit() {
  emit("save", {
    id: id.value.trim(),
    name: name.value.trim(),
    description: description.value,
    bindable: bindable.value,
    initialPlan: {
      id: planId.value.trim(),
      name: planName.value.trim(),
      description: planDescription.value,
      free: planFree.value,
      bindable: planBindable.value,
      schemas: {},
      parametersUiSchema: {},
    },
  });
  id.value = "";
  name.value = "";
  description.value = "";
  bindable.value = true;
  planId.value = "";
  planName.value = "";
  planDescription.value = "";
  planFree.value = true;
  planBindable.value = true;
}
</script>

<template>
  <form @submit.prevent="submit">
    <h3 class="section-title">{{ $t("offerings.offeringSection") }}</h3>
    <FormField :label="$t('common.id')">
      <IdInput v-model="id" required pattern="[a-z0-9-]+" />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <label class="checkbox-row">
      <input v-model="bindable" type="checkbox" />
      {{ $t("offerings.bindable") }}
    </label>

    <h3 class="section-title">{{ $t("offerings.initialPlan") }}</h3>
    <FormField :label="$t('plans.planId')">
      <IdInput v-model="planId" required pattern="[a-z0-9-]+" />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="planName" required />
    </FormField>
    <DescriptionField v-model="planDescription" />
    <label class="checkbox-row">
      <input v-model="planFree" type="checkbox" />
      {{ $t("common.free") }}
    </label>
    <label class="checkbox-row">
      <input v-model="planBindable" type="checkbox" />
      {{ $t("offerings.bindable") }}
    </label>
    <CreateButton type="submit" :label="$t('common.create')" />
  </form>
</template>

<style scoped>
.section-title {
  margin: 0 0 0.5rem;
  font-size: 0.95rem;
}
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.75rem;
  font-size: 0.9rem;
}
</style>
