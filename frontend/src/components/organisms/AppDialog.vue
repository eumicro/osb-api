<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import BaseButton from "../atoms/BaseButton.vue";

type DialogSize = "sm" | "md" | "lg";

const props = withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    size?: DialogSize;
    closeOnBackdrop?: boolean;
  }>(),
  { size: "md", closeOnBackdrop: true },
);

const emit = defineEmits<{ "update:open": [open: boolean]; close: [] }>();

const { t } = useI18n();
const titleId = `dialog-title-${Math.random().toString(36).slice(2, 9)}`;
const offset = ref({ x: 0, y: 0 });
const dragging = ref(false);
let dragOrigin = { x: 0, y: 0, pointerX: 0, pointerY: 0 };

const sizeClass = computed(() => `dialog-size-${props.size}`);

function close() {
  emit("update:open", false);
  emit("close");
}

function onBackdropClick() {
  if (props.closeOnBackdrop) close();
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") close();
}

function onDragStart(event: PointerEvent) {
  if ((event.target as HTMLElement).closest(".dialog-close")) return;
  dragging.value = true;
  dragOrigin = {
    x: offset.value.x,
    y: offset.value.y,
    pointerX: event.clientX,
    pointerY: event.clientY,
  };
  window.addEventListener("pointermove", onDragMove);
  window.addEventListener("pointerup", onDragEnd, { once: true });
}

function onDragMove(event: PointerEvent) {
  if (!dragging.value) return;
  offset.value = {
    x: dragOrigin.x + event.clientX - dragOrigin.pointerX,
    y: dragOrigin.y + event.clientY - dragOrigin.pointerY,
  };
}

function onDragEnd() {
  dragging.value = false;
  window.removeEventListener("pointermove", onDragMove);
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      offset.value = { x: 0, y: 0 };
      window.addEventListener("keydown", onKeydown);
      return;
    }
    window.removeEventListener("keydown", onKeydown);
  },
);

onUnmounted(() => {
  window.removeEventListener("keydown", onKeydown);
  window.removeEventListener("pointermove", onDragMove);
});
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dialog-overlay" @click.self="onBackdropClick">
      <section
        class="dialog-surface"
        :class="[sizeClass, { 'dialog-dragging': dragging }]"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        :style="{ transform: `translate(calc(-50% + ${offset.x}px), calc(-50% + ${offset.y}px))` }"
        @click.stop
      >
        <header class="dialog-header" @pointerdown="onDragStart">
          <h3 :id="titleId" class="dialog-title">{{ title }}</h3>
          <BaseButton
            class="dialog-close"
            icon="x"
            :label="t('dialog.close')"
            variant="secondary"
            @click="close"
          />
        </header>
        <div class="dialog-body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="dialog-footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>
