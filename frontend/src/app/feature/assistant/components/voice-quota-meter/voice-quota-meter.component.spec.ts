import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CREDITS_TICK_MS } from '../../models/credits.const';
import { VoiceQuotaDto } from '../../models/voice.model';
import { VoiceQuotaMeterComponent } from './voice-quota-meter.component';

function quota(used: number, overrides: Partial<VoiceQuotaDto> = {}): VoiceQuotaDto {
  return {
    dailyLimit: 40,
    used,
    remaining: Math.max(0, 40 - used),
    percentUsed: Math.min(100, Math.floor((used * 100) / 40)),
    monthlyExhausted: false,
    resetsAt: '2026-10-23T00:00:00-06:00',
    ...overrides,
  };
}

describe('VoiceQuotaMeterComponent', () => {
  let fixture: ComponentFixture<VoiceQuotaMeterComponent>;

  function create(value: VoiceQuotaDto): HTMLElement {
    fixture = TestBed.createComponent(VoiceQuotaMeterComponent);
    fixture.componentRef.setInput('quota', value);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-22T18:48:00-06:00'));
  });

  afterEach(() => vi.useRealTimers());

  it('shows dictations used, remaining and the time to the reset', () => {
    const el = create(quota(12));

    expect(el.textContent).toContain('12 de 40 dictados hoy');
    expect(el.textContent).toContain('28 restantes');
    expect(el.textContent).toContain('recarga en 5 h 12 min');
    expect(el.querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('30');
    expect(el.querySelector('.bg-primary-500')).not.toBeNull();
  });

  it('turns amber when few are left and red when none are', () => {
    expect(create(quota(34)).querySelector('.bg-amber-500')).not.toBeNull();

    const el = create(quota(40));
    expect(el.querySelector('.bg-rose-500')).not.toBeNull();
    expect(el.textContent).toContain('Sin dictados por hoy');
    expect(el.textContent).toContain('Se recargan en 5 h 12 min');
  });

  it('explains when the global monthly cap ran out', () => {
    const el = create(quota(3, { monthlyExhausted: true, remaining: 0, percentUsed: 100 }));

    expect(el.textContent).toContain('alcanzó su límite de este mes');
    expect(el.querySelector('[role="progressbar"]')).toBeNull();
  });

  it('emits expired once the reset moment passes', () => {
    create(quota(1));
    const expired = vi.fn();
    fixture.componentInstance.expired.subscribe(expired);

    vi.setSystemTime(new Date('2026-10-23T00:00:01-06:00'));
    vi.advanceTimersByTime(CREDITS_TICK_MS);

    expect(expired).toHaveBeenCalledTimes(1);
  });
});
