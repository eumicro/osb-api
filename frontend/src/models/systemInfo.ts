export interface SystemInfo {
  name: string;
  version: string;
  status: string;
  adapters: Record<string, string>;
  devServices: Record<string, string>;
}
