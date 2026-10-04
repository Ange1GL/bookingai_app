export const APP_ROUTES = {
  home: 'home',
  auth: 'auth',
  login: 'login',
  register: 'register',
} as const;

export const LOGIN_URL = `/${APP_ROUTES.auth}/${APP_ROUTES.login}`;
export const HOME_URL = `/${APP_ROUTES.home}`;
