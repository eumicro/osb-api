<script setup lang="ts">
import { ref } from "vue";
import { PANELS } from "../../workspace/panels";
import BaseButton from "../atoms/BaseButton.vue";

/** Molecule: Toggle which workspace panels are open in the current view. */
defineProps<{
  openPanelIds: string[];
}>();

defineEmits<{
  togglePanel: [panelId: string];
}>();

const menuOpen = ref(false);

function closeMenu() {
  menuOpen.value = false;
}
</script>

<template>
  <div class="menu panel-menu">
    <BaseButton
      icon="panels"
      :label="$t('views.panels')"
      variant="secondary"
      @click="menuOpen = !menuOpen"
    />

    <div v-if="menuOpen" class="dropdown panel-dropdown" @mouseleave="closeMenu">
      <section class="dropdown-section">
        <label v-for="panel in PANELS" :key="panel.id" class="dropdown-item">
          <input
            type="checkbox"
            :checked="openPanelIds.includes(panel.id)"
            @change="$emit('togglePanel', panel.id)"
          />
          {{ $t(panel.titleKey) }}
        </label>
      </section>
    </div>
  </div>
</template>
