import { NAVIGATION_PANELS } from "../workspace/panels";
import { focusPanel } from "../stores/workspaceLayout";

/** Controller: workspace navigation menu (opens/focuses list + dashboard panels). */
export function useNavigation() {
  const items = NAVIGATION_PANELS;

  function activate(panelId: string) {
    focusPanel(panelId);
  }

  return { items, activate };
}
