<script setup lang="ts">
import { computed, ref } from "vue";
import {
  LOCALE_FLAGS,
  LOCALE_LABELS,
  SUPPORTED_LOCALES,
  type AppLocale,
} from "../../i18n";

/** Molecule: UI language selection (flag + dropdown). */
const props = defineProps<{ locale: string }>();
const emit = defineEmits<{ change: [locale: AppLocale] }>();

const menuOpen = ref(false);

const currentLocale = computed<AppLocale>(() =>
  SUPPORTED_LOCALES.includes(props.locale as AppLocale)
    ? (props.locale as AppLocale)
    : "de",
);

function closeMenu() {
  menuOpen.value = false;
}

function select(next: AppLocale) {
  emit("change", next);
  closeMenu();
}
</script>

<template>
  <div class="menu">
    <button
      type="button"
      class="locale-trigger"
      :aria-label="$t('locale.label')"
      :title="$t('locale.label')"
      @click="menuOpen = !menuOpen"
    >
      <span class="locale-flag" aria-hidden="true">{{ LOCALE_FLAGS[currentLocale] }}</span>
    </button>
    <div
      v-if="menuOpen"
      class="dropdown align-end"
      role="menu"
      :aria-label="$t('locale.label')"
      @mouseleave="closeMenu"
    >
      <section class="dropdown-section">
        <button
          v-for="tag in SUPPORTED_LOCALES"
          :key="tag"
          type="button"
          class="dropdown-item as-text"
          :class="{ active: locale === tag }"
          role="menuitemradio"
          :aria-checked="locale === tag"
          @click="select(tag)"
        >
          <span class="locale-flag" aria-hidden="true">{{ LOCALE_FLAGS[tag] }}</span>
          {{ LOCALE_LABELS[tag] }}
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.locale-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  width: 2.15rem;
  height: 2.15rem;
  padding: 0;
  font: inherit;
  cursor: pointer;
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--border);
}

.locale-flag {
  font-size: 1.15rem;
  line-height: 1;
}

.dropdown-item.active {
  font-weight: 600;
  background: var(--accent-soft);
}
</style>
