import { environment } from '../../../environments/environment';

const AUTH_BASE = `${environment.apiBaseUrl}/auth`;

export const API_ENDPOINTS = {
  auth: {
    login: `${AUTH_BASE}/login`,
    register: `${AUTH_BASE}/register`,
    refresh: `${AUTH_BASE}/refresh`,
    logout: `${AUTH_BASE}/logout`,
  },
} as const;
