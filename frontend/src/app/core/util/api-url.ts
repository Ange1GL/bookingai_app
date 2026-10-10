import { environment } from '../../../environments/environment';

/**
 * Prefix shared by every backend endpoint. `apiBaseUrl` may be empty (same-origin behind CloudFront),
 * so matching on the host alone would treat every request as an API call.
 */
export const API_URL_PREFIX = `${environment.apiBaseUrl}/api/`;

export const isApiUrl = (url: string): boolean => url.startsWith(API_URL_PREFIX);
