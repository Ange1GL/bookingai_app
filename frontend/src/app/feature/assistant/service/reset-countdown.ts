import { computed, DestroyRef, inject, Signal, signal } from '@angular/core';
import { CREDITS_TICK_MS } from '../models/credits.const';
import { formatTimeUntil } from '../models/credits.util';

/**
 * Live "5 h 12 min" until a daily quota resets. Recomputed every {@link CREDITS_TICK_MS};
 * `onExpired` runs on each tick once the reset moment has passed (so the caller reloads the balance).
 * Must be called in an injection context: the timer is cleared with the owner.
 */
export function resetCountdown(resetsAt: () => string, onExpired: () => void): Signal<string> {
  const now = signal(Date.now());
  const timer = setInterval(() => {
    now.set(Date.now());
    if (now() >= Date.parse(resetsAt())) {
      onExpired();
    }
  }, CREDITS_TICK_MS);
  inject(DestroyRef).onDestroy(() => clearInterval(timer));
  return computed(() => formatTimeUntil(Date.parse(resetsAt()) - now()));
}
