import { parseUserAgent } from './parse-user-agent';

const CHROME_WINDOWS =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36';
const EDGE_WINDOWS = `${CHROME_WINDOWS} Edg/130.0.0.0`;
const FIREFOX_LINUX = 'Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0';
const SAFARI_MAC =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15';
const SAFARI_IPHONE =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1';
const CHROME_ANDROID =
  'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36';
const CHROME_ANDROID_TABLET =
  'Mozilla/5.0 (Linux; Android 13; SM-X700) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36';

describe('parseUserAgent', () => {
  it('reads Chrome on Windows as a desktop', () => {
    expect(parseUserAgent(CHROME_WINDOWS)).toEqual({ browser: 'Chrome', os: 'Windows', device: 'desktop' });
  });

  it('prefers Edge over Chrome', () => {
    expect(parseUserAgent(EDGE_WINDOWS).browser).toBe('Edge');
  });

  it('reads Firefox on Linux', () => {
    expect(parseUserAgent(FIREFOX_LINUX)).toEqual({ browser: 'Firefox', os: 'Linux', device: 'desktop' });
  });

  it('reads Safari on macOS', () => {
    expect(parseUserAgent(SAFARI_MAC)).toEqual({ browser: 'Safari', os: 'macOS', device: 'desktop' });
  });

  it('reads an iPhone as iOS mobile, not macOS', () => {
    expect(parseUserAgent(SAFARI_IPHONE)).toEqual({ browser: 'Safari', os: 'iOS', device: 'mobile' });
  });

  it('reads Chrome on an Android phone as mobile, not Linux', () => {
    expect(parseUserAgent(CHROME_ANDROID)).toEqual({ browser: 'Chrome', os: 'Android', device: 'mobile' });
  });

  it('reads an Android without "Mobile" as a tablet', () => {
    expect(parseUserAgent(CHROME_ANDROID_TABLET).device).toBe('tablet');
  });

  it('returns unknown for empty or unrecognised input', () => {
    const unknown = { browser: 'Desconocido', os: 'Desconocido', device: 'unknown' };
    expect(parseUserAgent(null)).toEqual(unknown);
    expect(parseUserAgent('   ')).toEqual(unknown);
    expect(parseUserAgent('curl/8.0')).toEqual(unknown);
  });
});
