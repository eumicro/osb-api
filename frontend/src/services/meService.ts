import type { Me } from "../models/me";
import { http } from "./http";

export const meService = {
  get: () => http.get<Me>("/bff/me"),
};
