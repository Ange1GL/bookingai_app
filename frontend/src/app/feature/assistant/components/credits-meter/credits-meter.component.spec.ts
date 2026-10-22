import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CREDITS_TICK_MS } from '../../models/credits.const';
import { CreditsDto } from '../../models/credits.model';
import { CreditsMeterComponent } from './credits-meter.component';

const NOW = new Date('2026-10-22T18:48:00-06:00');

function credits(used: number): CreditsDto {
  return {
    dailyLimit: 60000,
    used,
    remaining: Math.max(0, 60000 - used),
    percentUsed: Math.min(100, Math.floor((used * 100) / 60000)),
    resetsAt: '2026-10-23T00:00:00-06:00',
  };
}

describe('CreditsMeterComponent', () => {
  let fixture: ComponentFixture<CreditsMeterComponent>;

  function create(value: CreditsDto): HTMLElement {
    fixture = TestBed.createComponent(CreditsMeterComponent);
    fixture.componentRef.setInput('credits', value);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });

  afterEach(() => vi.useRealTimers());

  it('shows used, limit, remaining and the countdown', () => {
    const el = create(credits(25_400));

    expect(el.textContent).toContain('26 de 60 créditos usados hoy');
    expect(el.textContent).toContain('34 restantes');
    expect(el.textContent).toContain('en 5 h 12 min');
    expect(el.querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('42');
  });

  it('uses green, amber and red bars by level', () => {
    expect(create(credits(1000)).querySelector('.bg-emerald-500')).not.toBeNull();
    expect(create(credits(50_000)).querySelector('.bg-amber-500')).not.toBeNull();
    expect(create(credits(60_000)).querySelector('.bg-rose-500')).not.toBeNull();
  });

  it('warns when few credits are left', () => {
    expect(create(credits(50_000)).textContent).toContain('te quedan pocos');
  });

  it('updates the countdown and emits expired at the reset moment', () => {
    const el = create(credits(1000));
    const expired = vi.fn();
    fixture.componentInstance.expired.subscribe(expired);

    vi.setSystemTime(new Date('2026-10-22T23:30:00-06:00'));
    vi.advanceTimersByTime(CREDITS_TICK_MS);
    fixture.detectChanges();
    expect(el.textContent).toContain('en 30 min');
    expect(expired).not.toHaveBeenCalled();

    vi.setSystemTime(new Date('2026-10-23T00:00:01-06:00'));
    vi.advanceTimersByTime(CREDITS_TICK_MS);
    expect(expired).toHaveBeenCalledTimes(1);
  });
});
