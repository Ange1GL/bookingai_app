import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { LOW_CREDITS_PERCENT } from '../../models/credits.const';
import { VoiceQuotaDto } from '../../models/voice.model';
import { resetCountdown } from '../../service/reset-countdown';

/** Compact daily dictation balance shown above the composer: used / limit, bar and time to the reset. */
@Component({
  selector: 'app-voice-quota-meter',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './voice-quota-meter.component.html',
})
export class VoiceQuotaMeterComponent {
  readonly quota = input.required<VoiceQuotaDto>();
  /** The reset moment passed: the parent should reload the balance. */
  readonly expired = output<void>();

  protected readonly exhausted = computed(() => this.quota().remaining <= 0);
  protected readonly low = computed(() => !this.exhausted() && this.quota().percentUsed >= LOW_CREDITS_PERCENT);
  protected readonly barClass = computed(() => (this.exhausted() ? 'bg-rose-500' : this.low() ? 'bg-amber-500' : 'bg-primary-500'));
  protected readonly timeUntilReset = resetCountdown(
    () => this.quota().resetsAt,
    () => this.expired.emit(),
  );
}
