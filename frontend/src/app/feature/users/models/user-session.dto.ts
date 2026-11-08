/**
 * Mirrors UserSessionResponse. Timestamps are ISO-8601 instants. `lastActivityAt` only moves when the access token is
 * renewed (~every 15 min), so it is an approximation of the last activity, not live presence.
 */
export interface UserSessionDto {
  id: number;
  userAgent: string | null;
  ipAddress: string | null;
  startedAt: string;
  lastActivityAt: string;
  expiresAt: string;
}
