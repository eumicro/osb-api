import { readonly, ref } from "vue";

export type ConfirmOptions = {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmVariant?: "primary" | "danger";
};

const open = ref(false);
const options = ref<ConfirmOptions | null>(null);
let resolveFn: ((value: boolean) => void) | null = null;

/**
 * Controller: zentraler Bestaetigungsdialog (Promise-basiert).
 * Wird ueber DialogHost.vue im App-Root gerendert.
 */
export function useConfirm() {
  function confirm(opts: ConfirmOptions): Promise<boolean> {
    return new Promise((resolve) => {
      options.value = opts;
      open.value = true;
      resolveFn = resolve;
    });
  }

  function resolve(value: boolean) {
    open.value = false;
    resolveFn?.(value);
    resolveFn = null;
    options.value = null;
  }

  function onConfirm() {
    resolve(true);
  }

  function onCancel() {
    resolve(false);
  }

  return {
    open: readonly(open),
    options: readonly(options),
    confirm,
    onConfirm,
    onCancel,
  };
}
