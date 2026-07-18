export type GitClientAuthMethod = "HTTPS" | "SSH";

export interface GitClientInstance {
  id: string;
  name: string;
  description: string;
  remoteUrl: string;
  defaultBranch: string;
  authMethod: GitClientAuthMethod;
  username: string;
  secretConfigured: boolean;
  passphraseConfigured: boolean;
  enabled: boolean;
}

export interface CreateGitClientInstanceRequest {
  name: string;
  description: string;
  remoteUrl: string;
  defaultBranch: string;
  authMethod: GitClientAuthMethod;
  username: string;
  secret: string;
  passphrase: string;
  enabled: boolean;
}

export interface UpdateGitClientInstanceRequest {
  name: string;
  description: string;
  remoteUrl: string;
  defaultBranch: string;
  authMethod: GitClientAuthMethod;
  username: string;
  secret: string;
  passphrase: string | null;
  enabled: boolean;
}

export const GIT_CLIENT_AUTH_METHODS: readonly GitClientAuthMethod[] = ["HTTPS", "SSH"];
