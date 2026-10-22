import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { CREDITS_TICK_MS } from '../../models/credits.const';
import { CreditsDto } from '../../models/credits.model';
import { creditsLevel, formatTimeUntil, limitCredits, remainingCredits, usedCredits } from '../../models/credits.util';

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

  private readonly now = signal(Date.now());

  protected readonly level = computed(() => creditsLevel(this.credits()));
  protected readonly used = computed(() => usedCredits(this.credits()));
  protected readonly limit = computed(() => limitCredits(this.credits()));
  protected readonly remaining = computed(() => remainingCredits(this.credits()));
  protected readonly timeUntilReset = computed(() => formatTimeUntil(Date.parse(this.credits().resetsAt) - this.now()));
  protected readonly barClass = computed(() => BAR_CLASSES[this.level()]);

  constructor() {
    const timer = setInterval(() => this.tick(), CREDITS_TICK_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  private tick(): void {
    this.now.set(Date.now());
    if (this.now() >= Date.parse(this.credits().resetsAt)) {
      this.expired.emit();
    }
  }
}

const BAR_CLASSES = {
  ok: 'bg-emerald-500',
  low: 'bg-amber-500',
  exhausted: 'bg-rose-500',
} as const;
