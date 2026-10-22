import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, EMPTY } from 'rxjs';
import { LOW_QUOTA_WARNING } from '../models/voice.const';
import { VoiceQuotaDto } from '../models/voice.model';
import { AgentService } from './agent.service';

/** Daily dictation balance shared by the assistant page (meter) and the microphone button. */
@Injectable({ providedIn: 'root' })
export class VoiceQuotaStore {
  private readonly agent = inject(AgentService);
  private readonly quotaState = signal<VoiceQuotaDto | null>(null);

  /** `null` until the first load; dictation keeps working without it (the backend enforces the limit). */
  readonly quota = this.quotaState.asReadonly();
  readonly exhausted = computed(() => (this.quotaState()?.remaining ?? 1) <= 0);
  readonly low = computed(() => {
    const quota = this.quotaState();
    return !!quota && quota.remaining > 0 && quota.remaining <= LOW_QUOTA_WARNING;
  });

  /** Refreshes the balance; a failure keeps the last known value. */
  load(): void {
    this.agent
      .voiceQuota()
      .pipe(catchError(() => EMPTY))
      .subscribe((quota) => this.quotaState.set(quota));
  }

  /** After a successful dictation the response already says how many are left: no extra request. */
  applyRemaining(remaining: number): void {
    this.quotaState.update((quota) => {
      if (!quota) {
        return quota;
      }
      const used = Math.max(0, quota.dailyLimit - remaining);
      return { ...quota, used, remaining, percentUsed: Math.min(100, Math.floor((used * 100) / quota.dailyLimit)) };
    });
  }
}
