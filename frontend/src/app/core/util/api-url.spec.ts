import { environment } from '../../../environments/environment';
import { isApiUrl } from './api-url';

describe('isApiUrl', () => {
  it('matches backend endpoints', () => {
    expect(isApiUrl(`${environment.apiBaseUrl}/api/v1/customers`)).toBe(true);
  });

  it('does not match other URLs', () => {
    expect(isApiUrl('https://other.example.com/api/v1/x')).toBe(false);
    expect(isApiUrl('/assets/logo.svg')).toBe(false);
  });
});
