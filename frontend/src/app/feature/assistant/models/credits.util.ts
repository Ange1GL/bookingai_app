import { LOW_CREDITS_PERCENT, TOKENS_PER_CREDIT } from './credits.const';
import { CreditsDto, CreditsLevel } from './credits.model';

const MS_PER_MINUTE = 60_000;
const MINUTES_PER_HOUR = 60;

export function creditsLevel(credits: CreditsDto): CreditsLevel {
  if (credits.remaining <= 0) {
    return 'exhausted';
  }
  return credits.percentUsed >= LOW_CREDITS_PERCENT ? 'low' : 'ok';
}

/** Whole credits the user has used; rounded so tiny usage still shows. */
export function usedCredits(credits: CreditsDto): number {
  return Math.ceil(credits.used / TOKENS_PER_CREDIT);
}

export function limitCredits(credits: CreditsDto): number {
  return Math.round(credits.dailyLimit / TOKENS_PER_CREDIT);
}

/** Never shows more than what is really left. */
export function remainingCredits(credits: CreditsDto): number {
  return Math.floor(credits.remaining / TOKENS_PER_CREDIT);
}

/** "5 h 12 min", "42 min" or "menos de 1 min". */
export function formatTimeUntil(ms: number): string {
  const minutes = Math.ceil(ms / MS_PER_MINUTE);
  if (minutes <= 0) {
    return 'menos de 1 min';
  }
  const hours = Math.floor(minutes / MINUTES_PER_HOUR);
  const rest = minutes % MINUTES_PER_HOUR;
  if (hours === 0) {
    return `${rest} min`;
  }
  return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`;
}
