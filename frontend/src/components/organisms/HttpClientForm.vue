<script setup lang="ts">
import { computed, ref } from "vue";
import {
  HTTP_CLIENT_AUTH_TYPES,
  type CreateHttpClientInstanceRequest,
  type HttpClientAuthType,
} from "../../models/httpClient";
import BaseInput from "../atoms/BaseInput.vue";
import AuthTypeSelect from "../molecules/AuthTypeSelect.vue";
import ClientCredentialsFields from "../molecules/ClientCredentialsFields.vue";
import CreateButton from "../molecules/CreateButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const emit = defineEmits<{ save: [request: CreateHttpClientInstanceRequest] }>();

const name = ref("");
const description = ref("");
const baseUrl = ref("https://");
const authType = ref<HttpClientAuthType>("NONE");
const username = ref("");
const secret = ref("");
const oauthClientId = ref("");
const oauthClientSecret = ref("");
const wellKnownUrl = ref("");
const timeoutSeconds = ref("15");
const enabled = ref(true);

const needsUsername = computed(() => authType.value === "BASIC");
const needsSecret = computed(
  () => authType.value === "BASIC" || authType.value === "BEARER",
);
const needsClientCredentials = computed(() => authType.value === "CLIENT_CREDENTIALS");

function submit() {
  emit("save", {
    name: name.value.trim(),
    description: description.value.trim(),
    baseUrl: baseUrl.value.trim(),
    authType: authType.value,
    username: username.value.trim(),
    secret: needsSecret.value ? secret.value : "",
    oauthClientId: needsClientCredentials.value ? oauthClientId.value.trim() : "",
    oauthClientSecret: needsClientCredentials.value ? oauthClientSecret.value : "",
    wellKnownUrl: needsClientCredentials.value ? wellKnownUrl.value.trim() : "",
    timeoutSeconds: Number(timeoutSeconds.value) || 15,
    enabled: enabled.value,
  });
  name.value = "";
  description.value = "";
  baseUrl.value = "https://";
  authType.value = "NONE";
  username.value = "";
  secret.value = "";
  oauthClientId.value = "";
  oauthClientSecret.value = "";
  wellKnownUrl.value = "";
  timeoutSeconds.value = "15";
  enabled.value = true;
}
</script>

<template>
  <form @submit.prevent="submit">
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <FormField :label="$t('httpClients.baseUrl')">
      <BaseInput v-model="baseUrl" required placeholder="https://api.example.com" />
    </FormField>
    <AuthTypeSelect
      v-model="authType"
      :options="HTTP_CLIENT_AUTH_TYPES"
      label-key="httpClients.authType"
      option-key-prefix="httpClients.authTypes"
    />
    <FormField v-if="needsUsername" :label="$t('httpClients.username')">
      <BaseInput v-model="username" required />
    </FormField>
    <FormField v-if="needsSecret" :label="$t('httpClients.secret')">
      <BaseInput v-model="secret" type="password" required autocomplete="new-password" />
    </FormField>
    <ClientCredentialsFields
      v-if="needsClientCredentials"
      v-model:oauth-client-id="oauthClientId"
      v-model:oauth-client-secret="oauthClientSecret"
      v-model:well-known-url="wellKnownUrl"
      require-secret
    />
    <FormField :label="$t('httpClients.timeoutSeconds')">
      <BaseInput v-model="timeoutSeconds" type="number" min="1" max="300" required />
    </FormField>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("httpClients.enabled") }}
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
