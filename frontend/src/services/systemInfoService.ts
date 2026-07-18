import type { SystemInfo } from "../models/systemInfo";
import { http } from "./http";

export const systemInfoService = {
  getInfo: () => http.get<SystemInfo>("/api/info"),
};
