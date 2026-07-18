<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  HTTP_CLIENT_AUTH_TYPES,
  type HttpClientAuthType,
  type HttpClientInstance,
  type UpdateHttpClientInstanceRequest,
} from "../../models/httpClient";
import BaseInput from "../atoms/BaseInput.vue";
import AuthTypeSelect from "../molecules/AuthTypeSelect.vue";
import ClientCredentialsFields from "../molecules/ClientCredentialsFields.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const props = defineProps<{ httpClient: HttpClientInstance }>();
const emit = defineEmits<{ save: [request: UpdateHttpClientInstanceRequest] }>();

const name = ref(props.httpClient.name);
const description = ref(props.httpClient.description);
const baseUrl = ref(props.httpClient.baseUrl);
const authType = ref<HttpClientAuthType>(props.httpClient.authType);
const username = ref(props.httpClient.username);
const secret = ref("");
const oauthClientId = ref(props.httpClient.oauthClientId ?? "");
const oauthClientSecret = ref("");
const wellKnownUrl = ref(props.httpClient.wellKnownUrl ?? "");
const timeoutSeconds = ref(String(props.httpClient.timeoutSeconds));
const enabled = ref(props.httpClient.enabled);

const needsUsername = computed(() => authType.value === "BASIC");
const needsSecret = computed(
  () => authType.value === "BASIC" || authType.value === "BEARER",
);
const needsClientCredentials = computed(() => authType.value === "CLIENT_CREDENTIALS");

watch(
  () => props.httpClient,
  (client) => {
    name.value = client.name;
    description.value = client.description;
    baseUrl.value = client.baseUrl;
    authType.value = client.authType;
    username.value = client.username;
    secret.value = "";
    oauthClientId.value = client.oauthClientId ?? "";
    oauthClientSecret.value = "";
    wellKnownUrl.value = client.wellKnownUrl ?? "";
    timeoutSeconds.value = String(client.timeoutSeconds);
    enabled.value = client.enabled;
  },
);

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
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('common.id')">
      <BaseInput :model-value="httpClient.id" disabled />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <FormField :label="$t('httpClients.baseUrl')">
      <BaseInput v-model="baseUrl" required />
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
      <BaseInput
        v-model="secret"
        type="password"
        autocomplete="new-password"
        :placeholder="
          httpClient.secretConfigured
            ? $t('httpClients.secretKeepHint')
            : $t('httpClients.secret')
        "
      />
    </FormField>
    <ClientCredentialsFields
      v-if="needsClientCredentials"
      v-model:oauth-client-id="oauthClientId"
      v-model:oauth-client-secret="oauthClientSecret"
      v-model:well-known-url="wellKnownUrl"
      :secret-configured="httpClient.oauthClientSecretConfigured"
    />
    <FormField :label="$t('httpClients.timeoutSeconds')">
      <BaseInput v-model="timeoutSeconds" type="number" min="1" max="300" required />
    </FormField>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("httpClients.enabled") }}
    </label>
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
</style>
