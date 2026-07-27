<script setup lang="ts">
import { ref, watch } from "vue";
import type { AdminCatalog } from "../../models/catalog";
import type { PlatformClient, UpdatePlatformClientRequest } from "../../models/platformClient";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import FormField from "../molecules/FormField.vue";
import PlatformConnectionInfo from "./PlatformConnectionInfo.vue";

const props = defineProps<{
  platform: PlatformClient;
  catalogs: AdminCatalog[];
  revealedPassword: string | null;
}>();
const emit = defineEmits<{ save: [request: UpdatePlatformClientRequest] }>();

const displayName = ref(props.platform.displayName);
const username = ref(props.platform.username);
const catalogId = ref(props.platform.catalogId);
const password = ref("");
const enabled = ref(props.platform.enabled);

watch(
  () => props.platform,
  (platform) => {
    displayName.value = platform.displayName;
    username.value = platform.username;
    catalogId.value = platform.catalogId;
    password.value = "";
    enabled.value = platform.enabled;
  },
);

function submit() {
  const request: UpdatePlatformClientRequest = {
    displayName: displayName.value.trim(),
    username: username.value.trim(),
    catalogId: catalogId.value,
    enabled: enabled.value,
  };
  if (password.value) {
    request.password = password.value;
  }
  emit("save", request);
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('common.id')">
      <BaseInput :model-value="platform.id" disabled />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="displayName" required />
    </FormField>
    <FormField :label="$t('platforms.username')">
      <BaseInput v-model="username" required />
    </FormField>
    <FormField :label="$t('platforms.password')">
      <BaseInput
        v-model="password"
        type="password"
        autocomplete="new-password"
        :placeholder="
          platform.passwordConfigured
            ? $t('platforms.passwordKeepHint')
            : $t('platforms.password')
        "
        :required="!platform.passwordConfigured"
      />
    </FormField>
    <FormField :label="$t('platforms.catalog')">
      <BaseSelect v-model="catalogId" required>
        <option v-for="catalog in catalogs" :key="catalog.id" :value="catalog.id">
          {{ catalog.name }} ({{ catalog.id }})
        </option>
      </BaseSelect>
    </FormField>
    <p class="muted">{{ $t("platforms.catalogHint") }}</p>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("platforms.enabled") }}
    </label>

    <PlatformConnectionInfo :platform="platform" :revealed-password="revealedPassword" />
  </div>
</template>

<style scoped>
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.85rem;
  font-size: 0.9rem;
}

.muted {
  color: var(--muted);
  font-size: 0.85rem;
  margin: 0 0 0.75rem;
}
</style>
