export interface PlatformClient {
  id: string;
  displayName: string;
  username: string;
  catalogId: string;
  passwordConfigured: boolean;
  enabled: boolean;
}

export interface CreatePlatformClientRequest {
  displayName: string;
  username: string;
  catalogId: string;
  password: string;
  enabled: boolean;
}

export interface UpdatePlatformClientRequest {
  displayName: string;
  username: string;
  catalogId: string;
  password?: string;
  enabled: boolean;
}
