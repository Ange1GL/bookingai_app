/** Mirrors the backend CreditsResponse record (amounts are provider tokens). */
export interface CreditsDto {
  dailyLimit: number;
  used: number;
  remaining: number;
  percentUsed: number;
  /** ISO date-time with offset: when the daily counter goes back to zero. */
  resetsAt: string;
}

/** Drives the meter colour: `low` warns before the limit, `exhausted` blocks the composer. */
export type CreditsLevel = 'ok' | 'low' | 'exhausted';
