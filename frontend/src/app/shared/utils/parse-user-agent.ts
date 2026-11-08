export type DeviceKind = 'desktop' | 'mobile' | 'tablet' | 'unknown';

export interface ParsedUserAgent {
  browser: string;
  os: string;
  device: DeviceKind;
}

interface Rule {
  name: string;
  pattern: RegExp;
}

const UNKNOWN = 'Desconocido';

// Order matters: Edge and Opera also say "Chrome", and Chrome also says "Safari".
const BROWSER_RULES: readonly Rule[] = [
  { name: 'Edge', pattern: /Edg(e|A|iOS)?\//i },
  { name: 'Opera', pattern: /OPR\/|Opera/i },
  { name: 'Firefox', pattern: /Firefox\/|FxiOS\//i },
  { name: 'Chrome', pattern: /Chrome\/|CriOS\//i },
  { name: 'Safari', pattern: /Safari\//i },
];

// Order matters: iPhone/iPad user agents contain "like Mac OS X", Android ones contain "Linux".
const OS_RULES: readonly Rule[] = [
  { name: 'Windows', pattern: /Windows/i },
  { name: 'iOS', pattern: /iPhone|iPad|iPod/i },
  { name: 'Android', pattern: /Android/i },
  { name: 'macOS', pattern: /Macintosh|Mac OS X/i },
  { name: 'Linux', pattern: /Linux|X11/i },
];

const TABLET_PATTERN = /iPad|Tablet|Android(?!.*Mobile)/i;
const MOBILE_PATTERN = /Mobi|iPhone|iPod|Android/i;

function firstMatch(rules: readonly Rule[], userAgent: string): string {
  return rules.find((rule) => rule.pattern.test(userAgent))?.name ?? UNKNOWN;
}

function detectDevice(userAgent: string): DeviceKind {
  if (TABLET_PATTERN.test(userAgent)) {
    return 'tablet';
  }
  if (MOBILE_PATTERN.test(userAgent)) {
    return 'mobile';
  }
  return /Windows|Macintosh|Linux|X11/i.test(userAgent) ? 'desktop' : 'unknown';
}

/** Best-effort reading of a raw User-Agent header; anything unrecognised comes back as "Desconocido". */
export function parseUserAgent(userAgent: string | null | undefined): ParsedUserAgent {
  if (!userAgent?.trim()) {
    return { browser: UNKNOWN, os: UNKNOWN, device: 'unknown' };
  }
  return { browser: firstMatch(BROWSER_RULES, userAgent), os: firstMatch(OS_RULES, userAgent), device: detectDevice(userAgent) };
}
