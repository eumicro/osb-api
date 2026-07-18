<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  GIT_CLIENT_AUTH_METHODS,
  type CreateGitClientInstanceRequest,
  type GitClientAuthMethod,
} from "../../models/gitClient";
import BaseInput from "../atoms/BaseInput.vue";
import BaseSelect from "../atoms/BaseSelect.vue";
import CreateButton from "../molecules/CreateButton.vue";
import DescriptionField from "../molecules/DescriptionField.vue";
import FormField from "../molecules/FormField.vue";

const emit = defineEmits<{ save: [request: CreateGitClientInstanceRequest] }>();

const name = ref("");
const description = ref("");
const authMethod = ref<GitClientAuthMethod>("HTTPS");
const remoteUrl = ref("https://");
const defaultBranch = ref("main");
const username = ref("");
const secret = ref("");
const passphrase = ref("");
const enabled = ref(true);

const isSsh = computed(() => authMethod.value === "SSH");

watch(authMethod, (method) => {
  if (method === "HTTPS" && !remoteUrl.value.startsWith("http")) {
    remoteUrl.value = "https://";
  }
  if (method === "SSH" && !remoteUrl.value.startsWith("git@") && !remoteUrl.value.startsWith("ssh://")) {
    remoteUrl.value = "git@";
    if (!username.value) username.value = "git";
  }
});

function submit() {
  emit("save", {
    name: name.value.trim(),
    description: description.value.trim(),
    remoteUrl: remoteUrl.value.trim(),
    defaultBranch: defaultBranch.value.trim() || "main",
    authMethod: authMethod.value,
    username: username.value.trim(),
    secret: secret.value,
    passphrase: isSsh.value ? passphrase.value : "",
    enabled: enabled.value,
  });
  name.value = "";
  description.value = "";
  authMethod.value = "HTTPS";
  remoteUrl.value = "https://";
  defaultBranch.value = "main";
  username.value = "";
  secret.value = "";
  passphrase.value = "";
  enabled.value = true;
}
</script>

<template>
  <form @submit.prevent="submit">
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
      <BaseInput
        v-model="remoteUrl"
        required
        :placeholder="isSsh ? 'git@host:org/repo.git' : 'https://host/org/repo.git'"
      />
    </FormField>
    <FormField :label="$t('gitClients.defaultBranch')">
      <BaseInput v-model="defaultBranch" required />
    </FormField>
    <FormField :label="$t('gitClients.username')">
      <BaseInput
        v-model="username"
        :placeholder="isSsh ? 'git' : $t('gitClients.usernameHttpsHint')"
      />
    </FormField>
    <FormField :label="isSsh ? $t('gitClients.privateKey') : $t('gitClients.passwordOrToken')">
      <BaseInput
        v-if="!isSsh"
        v-model="secret"
        type="password"
        required
        autocomplete="new-password"
      />
      <textarea
        v-else
        v-model="secret"
        class="key-area"
        required
        rows="5"
        :placeholder="$t('gitClients.privateKeyPlaceholder')"
      />
    </FormField>
    <FormField v-if="isSsh" :label="$t('gitClients.passphrase')">
      <BaseInput v-model="passphrase" type="password" autocomplete="new-password" />
    </FormField>
    <label class="checkbox-row">
      <input v-model="enabled" type="checkbox" />
      {{ $t("gitClients.enabled") }}
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
