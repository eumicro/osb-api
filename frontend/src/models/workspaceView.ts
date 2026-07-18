/** Eine benannte Workspace-Ansicht mit eigenem Dockview-Layout. */
export interface WorkspaceView {
  id: string;
  name: string;
  layout: object;
}

/** Persistierter Zustand aller Ansichten eines Benutzers. */
export interface WorkspaceViewsState {
  activeViewId: string;
  views: WorkspaceView[];
}

export const VIEWS_CONFIG_KEY = "workspace-views";
export const LEGACY_LAYOUT_KEY = "workspace-layout";
