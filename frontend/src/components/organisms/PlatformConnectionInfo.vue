<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { brokerCatalogUrl, brokerPublicUrl } from "../../config/brokerPublicUrl";
import type { PlatformClient } from "../../models/platformClient";
import CopyableField from "../molecules/CopyableField.vue";

const props = defineProps<{
  platform: PlatformClient;
  revealedPassword: string | null;
}>();

const { t } = useI18n();
const copyAllDone = ref(false);
let copyAllTimer: ReturnType<typeof setTimeout> | undefined;

const apiBaseUrl = computed(() => brokerPublicUrl());
const catalogUrl = computed(() => brokerCatalogUrl());

const connectionSnippet = computed(() => {
  const lines = [
    `API: ${apiBaseUrl.value}`,
    `Catalog: ${catalogUrl.value}`,
    `Username: ${props.platform.username}`,
  ];
  if (props.revealedPassword) {
    lines.push(`Password: ${props.revealedPassword}`);
  }
  lines.push("Header: X-Broker-API-Version: 2.17");
  lines.push("Auth: HTTP Basic");
  return lines.join("\n");
});

async function copyAll() {
  try {
    await navigator.clipboard.writeText(connectionSnippet.value);
    copyAllDone.value = true;
    if (copyAllTimer) clearTimeout(copyAllTimer);
    copyAllTimer = setTimeout(() => {
      copyAllDone.value = false;
    }, 1500);
  } catch {
    copyAllDone.value = false;
  }
}
</script>

<template>
  <section class="connection" aria-labelledby="platform-connection-title">
    <h3 id="platform-connection-title" class="connection__title">
      {{ t("platforms.connectionTitle") }}
    </h3>
    <p class="muted">{{ t("platforms.apiVersionHint") }}</p>

    <CopyableField :label="t('platforms.apiBaseUrl')" :value="apiBaseUrl" />
    <CopyableField :label="t('platforms.catalogUrl')" :value="catalogUrl" />
    <CopyableField :label="t('platforms.username')" :value="platform.username" />

    <CopyableField
      v-if="revealedPassword"
      :label="t('platforms.password')"
      :value="revealedPassword"
      secret
    />
    <p v-if="revealedPassword" class="muted">{{ t("platforms.passwordHandedOff") }}</p>
    <p v-else-if="platform.passwordConfigured" class="muted">
      {{ t("platforms.passwordNotReadable") }}
    </p>
    <p v-else class="muted">{{ t("platforms.passwordRequiredHint") }}</p>

    <button type="button" class="connection__copy-all" @click="copyAll">
      {{ copyAllDone ? t("platforms.copied") : t("platforms.copyAll") }}
    </button>
  </section>
</template>

<style scoped>
.connection {
  margin-top: 1.25rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border);
}

.connection__title {
  margin: 0 0 0.35rem;
  font-size: 0.95rem;
  font-weight: 650;
}

.connection__copy-all {
  margin-top: 0.35rem;
  padding: 0.45rem 0.75rem;
  font: inherit;
  font-size: 0.85rem;
  cursor: pointer;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text);
}

.connection__copy-all:hover {
  border-color: var(--accent);
}

.muted {
  color: var(--muted);
  font-size: 0.85rem;
  margin: 0 0 0.75rem;
}
</style>
