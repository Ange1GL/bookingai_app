import { InjectionToken } from '@angular/core';

const IOS_USER_AGENT = /iphone|ipad|ipod/i;
const MAC_PLATFORM = /mac/i;
const IPADOS_MIN_TOUCH_POINTS = 1;
const STANDALONE_QUERY = '(display-mode: standalone)';

interface StandaloneNavigator extends Navigator {
  /** Safari-only flag, true when launched from the home screen. */
  standalone?: boolean;
}

/** iPhone, iPad and iPadOS (which reports itself as a Mac but has a touch screen). */
export function isIos(): boolean {
  if (typeof navigator === 'undefined') {
    return false;
  }
  const ipadOs = MAC_PLATFORM.test(navigator.platform) && navigator.maxTouchPoints > IPADOS_MIN_TOUCH_POINTS;
  return IOS_USER_AGENT.test(navigator.userAgent) || ipadOs;
}

/** Running as an installed app (home-screen icon) instead of a browser tab. */
export function isStandalone(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  return window.matchMedia(STANDALONE_QUERY).matches || (navigator as StandaloneNavigator).standalone === true;
}

/** The three browser APIs Web Push needs. */
export function supportsPush(): boolean {
  return (
    typeof navigator !== 'undefined' &&
    typeof window !== 'undefined' &&
    'serviceWorker' in navigator &&
    'PushManager' in window &&
    'Notification' in window
  );
}

export function notificationPermission(): NotificationPermission {
  return typeof Notification === 'undefined' ? 'default' : Notification.permission;
}

/** Browser capabilities behind an injection token, so tests can simulate an iPhone, a blocked permission, etc. */
export interface PwaEnvironment {
  isIos(): boolean;
  isStandalone(): boolean;
  supportsPush(): boolean;
  notificationPermission(): NotificationPermission;
}

export const PWA_ENVIRONMENT = new InjectionToken<PwaEnvironment>('PWA_ENVIRONMENT', {
  providedIn: 'root',
  factory: () => ({ isIos, isStandalone, supportsPush, notificationPermission }),
});
