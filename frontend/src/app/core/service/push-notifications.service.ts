import { HttpClient, HttpContext } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { SwPush } from '@angular/service-worker';
import { catchError, from, map, Observable, of, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../interceptor/error.interceptor';
import { SKIP_LOADER } from '../interceptor/loader.interceptor';
import { PushPublicKeyDto, PushStatus, PushTestResponseDto } from '../model/push.model';
import { PWA_ENVIRONMENT } from '../util/pwa.util';

/**
 * Appointment reminders via Web Push: asks the browser for permission, subscribes this device and registers
 * the subscription in the backend (which sends the push before each appointment). The card that uses it shows
 * its own errors, so the global toast and loader stay out.
 */
@Injectable({ providedIn: 'root' })
export class PushNotificationsService {
  private readonly http = inject(HttpClient);
  private readonly swPush = inject(SwPush);
  private readonly pwa = inject(PWA_ENVIRONMENT);
  private readonly url = `${environment.apiBaseUrl}/api/v1/push`;
  private readonly options = { context: new HttpContext().set(SKIP_LOADER, true).set(SKIP_ERROR_TOAST, true) };

  private readonly subscription = toSignal(this.swPush.subscription, { initialValue: null });
  private readonly permission = signal(this.pwa.notificationPermission());
  /** Endpoint already registered in the backend during this session (avoids re-posting on every visit). */
  private registeredEndpoint: string | null = null;

  readonly status = computed<PushStatus>(() => {
    // Safari tabs on iPhone expose no PushManager at all: tell the user to install the app first.
    if (this.pwa.isIos() && !this.pwa.isStandalone()) {
      return 'needs-install';
    }
    if (!this.pwa.supportsPush() || !this.swPush.isEnabled) {
      return 'unsupported';
    }
    if (this.permission() === 'denied') {
      return 'blocked';
    }
    return this.subscription() ? 'on' : 'off';
  });

  /** Must run from a user gesture (a click): iOS refuses to show the permission prompt otherwise. */
  enable(): Observable<void> {
    return this.http.get<PushPublicKeyDto>(`${this.url}/public-key`, this.options).pipe(
      switchMap(({ publicKey }) => from(this.swPush.requestSubscription({ serverPublicKey: publicKey }))),
      switchMap((subscription) => this.register(subscription)),
      tap(() => this.permission.set(this.pwa.notificationPermission())),
      catchError((error: unknown) => {
        // A denied prompt flips the status to `blocked`; any other failure leaves it as it was.
        this.permission.set(this.pwa.notificationPermission());
        return throwError(() => error);
      }),
    );
  }

  disable(): Observable<void> {
    const current = this.subscription();
    if (!current) {
      return of(undefined);
    }
    return this.http.delete<void>(`${this.url}/subscriptions`, { ...this.options, body: { endpoint: current.endpoint } }).pipe(
      // If the backend call fails the row is cleaned up on its own the first time the push service answers 410.
      catchError(() => of(undefined)),
      switchMap(() => from(this.swPush.unsubscribe())),
      tap(() => (this.registeredEndpoint = null)),
    );
  }

  sendTest(): Observable<PushTestResponseDto> {
    return this.http.post<PushTestResponseDto>(`${this.url}/test`, null, this.options);
  }

  /**
   * Re-registers an existing subscription once per session. The backend upserts by endpoint, so this also moves
   * the device to the current user when someone else signs in from the same browser.
   */
  syncExisting(): Observable<void> {
    const current = this.subscription();
    if (!current || current.endpoint === this.registeredEndpoint) {
      return of(undefined);
    }
    return this.register(current);
  }

  private register(subscription: PushSubscription): Observable<void> {
    return this.http.post<void>(`${this.url}/subscriptions`, subscription.toJSON(), this.options).pipe(
      tap(() => (this.registeredEndpoint = subscription.endpoint)),
      map(() => undefined),
    );
  }
}
