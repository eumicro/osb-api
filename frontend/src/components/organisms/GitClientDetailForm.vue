<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  GIT_CLIENT_AUTH_METHODS,
  type GitClientAuthMethod,
  type GitClientInstance,
  type UpdateGitClientInstanceRequest,
} from "../../models/gitClient";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const props = defineProps<{ gitClient: GitClientInstance }>();
const emit = defineEmits<{ save: [request: UpdateGitClientInstanceRequest] }>();

const name = ref(props.gitClient.name);
const description = ref(props.gitClient.description);
const authMethod = ref<GitClientAuthMethod>(props.gitClient.authMethod);
const remoteUrl = ref(props.gitClient.remoteUrl);
const defaultBranch = ref(props.gitClient.defaultBranch);
const username = ref(props.gitClient.username);
const secret = ref("");
const passphrase = ref("");
const clearPassphrase = ref(false);
const enabled = ref(props.gitClient.enabled);

const isSsh = computed(() => authMethod.value === "SSH");

watch(
  () => props.gitClient,
  (client) => {
    name.value = client.name;
    description.value = client.description;
    authMethod.value = client.authMethod;
    remoteUrl.value = client.remoteUrl;
    defaultBranch.value = client.defaultBranch;
    username.value = client.username;
    secret.value = "";
    passphrase.value = "";
    clearPassphrase.value = false;
    enabled.value = client.enabled;
  },
);

function submit() {
  emit("save", {
    name: name.value.trim(),
    description: description.value.trim(),
    remoteUrl: remoteUrl.value.trim(),
    defaultBranch: defaultBranch.value.trim() || "main",
    authMethod: authMethod.value,
    username: username.value.trim(),
    secret: secret.value,
    passphrase: isSsh.value
      ? clearPassphrase.value
        ? ""
        : passphrase.value === ""
          ? null
          : passphrase.value
      : null,
    enabled: enabled.value,
  });
}

defineExpose({ submit });
</script>

<template>
  <div class="detail-fields">
    <FormField :label="$t('common.id')">
      <BaseInput :model-value="gitClient.id" disabled />
    </FormField>
    <FormField :label="$t('common.name')">
      <BaseInput v-model="name" required />
    </FormField>
    <DescriptionField v-model="description" />
    <FormField :label="$t('gitClients.authMethod')">
      <BaseSelect v-model="authMethod" required>
        <option v-for="entry in GIT_CLIENT_AUTH_METHODS" :key="entry" :value="entry">
          {{ $t(`gitClients.authMethods.${entry}`) }}
        </option>
      </BaseSelect>
    </FormField>
    <FormField :label="$t('gitClients.remoteUrl')">
      <BaseInput v-model="remoteUrl" required />
    </FormField>
    <FormField :label="$t('gitClients.defaultBranch')">
      <BaseInput v-model="defaultBranch" required />
    </FormField>
    <FormField :label="$t('gitClients.username')">
      <BaseInput v-model="username" />
    </FormField>
    <FormField :label="isSsh ? $t('gitClients.privateKey') : $t('gitClients.passwordOrToken')">
      <BaseInput
        v-if="!isSsh"
        v-model="secret"
        type="password"
        autocomplete="new-password"
        :placeholder="
          gitClient.secretConfigured
            ? $t('gitClients.secretKeepHint')
            : $t('gitClients.passwordOrToken')
        "
      />
      <textarea
        v-else
        v-model="secret"
        class="key-area"
        rows="5"
        :placeholder="
          gitClient.secretConfigured
            ? $t('gitClients.secretKeepHint')
            : $t('gitClients.privateKeyPlaceholder')
        "
      />
    </FormField>
    <template v-if="isSsh">
      <FormField :label="$t('gitClients.passphrase')">
        <BaseInput
          v-model="passphrase"
          type="password"
          autocomplete="new-password"
          :disabled="clearPassphrase"
          :placeholder="
            gitClient.passphraseConfigured
              ? $t('gitClients.passphraseKeepHint')
              : $t('gitClients.passphrase')
          "
        />
      </FormField>
      <label v-if="gitClient.passphraseConfigured" class="checkbox-row">
        <input v-model="clearPassphrase" type="checkbox" />
        {{ $t("gitClients.clearPassphrase") }}
      </label>
    </template>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("gitClients.enabled") }}
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
.key-area {
  width: 100%;
  font-family: ui-monospace, monospace;
  font-size: 0.8rem;
  padding: 0.45rem 0.55rem;
  border-radius: 0.35rem;
  border: 1px solid var(--border, #ccc);
  background: var(--input-bg, #fff);
  color: inherit;
  resize: vertical;
}
</style>
