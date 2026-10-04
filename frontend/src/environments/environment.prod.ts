// Same-origin path: serve the SPA and the API behind the same host (reverse proxy)
// so HttpOnly auth cookies and the CSRF cookie work. Change only if the API is
// exposed on another origin that is allowed by the backend CORS configuration.
export const environment = {
  production: true,
  apiBaseUrl: '/api/v1',
};
