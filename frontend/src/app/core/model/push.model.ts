/** Mirrors the backend PushPublicKeyResponse record. */
export interface PushPublicKeyDto {
  publicKey: string;
}

/** Mirrors the backend PushTestResponse record. */
export interface PushTestResponseDto {
  delivered: number;
}

/**
 * Where the user is with appointment reminders on this device:
 * - `unsupported`: no Web Push here (old browser, or the service worker is off in `ng serve`)
 * - `needs-install`: iPhone/iPad in a Safari tab; push only works from the home-screen app
 * - `blocked`: the user denied the permission; only the browser settings can undo that
 * - `off` / `on`: supported, not subscribed / subscribed
 */
export type PushStatus = 'unsupported' | 'needs-install' | 'blocked' | 'off' | 'on';

/** Mirrors `app.push.lead-minutes` in the backend; only used in copy shown to the user. */
export const REMINDER_LEAD_MINUTES = 5;
