import { CreditsDto } from './credits.model';
import { creditsLevel, formatTimeUntil, limitCredits, remainingCredits, usedCredits } from './credits.util';

function credits(used: number, dailyLimit = 60000): CreditsDto {
  const remaining = Math.max(0, dailyLimit - used);
  return {
    dailyLimit,
    used,
    remaining,
    percentUsed: Math.min(100, Math.floor((used * 100) / dailyLimit)),
    resetsAt: '2026-10-23T00:00:00-06:00',
  };
}

describe('credits.util', () => {
  it('derives the level from usage', () => {
    expect(creditsLevel(credits(0))).toBe('ok');
    expect(creditsLevel(credits(47_999))).toBe('ok');
    expect(creditsLevel(credits(48_000))).toBe('low');
    expect(creditsLevel(credits(60_000))).toBe('exhausted');
    expect(creditsLevel(credits(75_000))).toBe('exhausted');
  });

  it('expresses amounts in credits without overstating what is left', () => {
    const value = credits(25_400);
    expect(usedCredits(value)).toBe(26);
    expect(limitCredits(value)).toBe(60);
    expect(remainingCredits(value)).toBe(34);
  });

  it('formats the time until the reset', () => {
    expect(formatTimeUntil(5 * 3_600_000 + 12 * 60_000)).toBe('5 h 12 min');
    expect(formatTimeUntil(2 * 3_600_000)).toBe('2 h');
    expect(formatTimeUntil(42 * 60_000)).toBe('42 min');
    expect(formatTimeUntil(0)).toBe('menos de 1 min');
    expect(formatTimeUntil(-5000)).toBe('menos de 1 min');
  });
});
