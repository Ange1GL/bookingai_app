import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { VoiceQuotaDto } from '../models/voice.model';
import { AgentService } from './agent.service';
import { VoiceQuotaStore } from './voice-quota.store';

const QUOTA: VoiceQuotaDto = {
  dailyLimit: 40,
  used: 10,
  remaining: 30,
  percentUsed: 25,
  monthlyExhausted: false,
  resetsAt: '2026-10-23T00:00:00-06:00',
};

describe('VoiceQuotaStore', () => {
  let voiceQuota: ReturnType<typeof vi.fn<() => Observable<VoiceQuotaDto>>>;
  let store: VoiceQuotaStore;

  beforeEach(() => {
    voiceQuota = vi.fn(() => of(QUOTA));
    TestBed.configureTestingModule({ providers: [{ provide: AgentService, useValue: { voiceQuota } }] });
    store = TestBed.inject(VoiceQuotaStore);
  });

  it('starts empty and is not exhausted until it knows better', () => {
    expect(store.quota()).toBeNull();
    expect(store.exhausted()).toBe(false);
  });

  it('loads the balance', () => {
    store.load();

    expect(store.quota()).toEqual(QUOTA);
    expect(store.low()).toBe(false);
  });

  it('keeps the last value when the load fails', () => {
    store.load();
    voiceQuota.mockReturnValueOnce(throwError(() => new Error('down')));

    store.load();

    expect(store.quota()).toEqual(QUOTA);
  });

  it('updates the balance from a dictation response without another request', () => {
    store.load();

    store.applyRemaining(4);

    expect(store.quota()).toMatchObject({ used: 36, remaining: 4, percentUsed: 90 });
    expect(store.low()).toBe(true);
    expect(voiceQuota).toHaveBeenCalledTimes(1);
  });

  it('is exhausted when nothing is left or the monthly cap ran out', () => {
    store.load();
    store.applyRemaining(0);
    expect(store.exhausted()).toBe(true);

    voiceQuota.mockReturnValueOnce(of({ ...QUOTA, remaining: 0, monthlyExhausted: true }));
    store.load();
    expect(store.exhausted()).toBe(true);
  });

  it('ignores a dictation response before the balance is known', () => {
    store.applyRemaining(4);

    expect(store.quota()).toBeNull();
  });
});
