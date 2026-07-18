export interface PlatformClient {
  id: string;
  displayName: string;
  username: string;
  catalogId: string;
  enabled: boolean;
}

export interface CreatePlatformClientRequest {
  displayName: string;
  username: string;
  catalogId: string;
  enabled: boolean;
}

export interface UpdatePlatformClientRequest {
  displayName: string;
  username: string;
  catalogId: string;
  enabled: boolean;
}
