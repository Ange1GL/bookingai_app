import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CreditsDto } from '../../models/credits.model';
import { creditsLevel, limitCredits, remainingCredits, usedCredits } from '../../models/credits.util';
import { resetCountdown } from '../../service/reset-countdown';

/** Daily AI credits: used / limit, a colour-coded bar and the countdown to the next reset. */
@Component({
  selector: 'app-credits-meter',
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './credits-meter.component.html',
})
export class CreditsMeterComponent {
  readonly credits = input.required<CreditsDto>();
  /** The reset moment passed: the parent should reload the balance. */
  readonly expired = output<void>();

  protected readonly level = computed(() => creditsLevel(this.credits()));
  protected readonly used = computed(() => usedCredits(this.credits()));
  protected readonly limit = computed(() => limitCredits(this.credits()));
  protected readonly remaining = computed(() => remainingCredits(this.credits()));
  protected readonly timeUntilReset = resetCountdown(
    () => this.credits().resetsAt,
    () => this.expired.emit(),
  );
  protected readonly barClass = computed(() => BAR_CLASSES[this.level()]);
}

const BAR_CLASSES = {
  ok: 'bg-emerald-500',
  low: 'bg-amber-500',
  exhausted: 'bg-rose-500',
} as const;
