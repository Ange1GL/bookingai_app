import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SwPush } from '@angular/service-worker';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../interceptor/error.interceptor';
import { PWA_ENVIRONMENT, PwaEnvironment } from '../util/pwa.util';
import { PushNotificationsService } from './push-notifications.service';

const BASE = `${environment.apiBaseUrl}/api/v1/push`;
const ENDPOINT = 'https://fcm.googleapis.com/fcm/send/abc';

function fakeSubscription(endpoint = ENDPOINT): PushSubscription {
  const json = { endpoint, expirationTime: null, keys: { p256dh: 'p', auth: 'a' } };
  return { endpoint, toJSON: () => json } as unknown as PushSubscription;
}

describe('PushNotificationsService', () => {
  let http: HttpTestingController;
  let subscription$: BehaviorSubject<PushSubscription | null>;
  let swPush: {
    isEnabled: boolean;
    subscription: BehaviorSubject<PushSubscription | null>;
    requestSubscription: ReturnType<typeof vi.fn<(options: { serverPublicKey: string }) => Promise<PushSubscription>>>;
    unsubscribe: ReturnType<typeof vi.fn<() => Promise<void>>>;
  };
  let pwa: PwaEnvironment;

  function create(): PushNotificationsService {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SwPush, useValue: swPush },
        { provide: PWA_ENVIRONMENT, useValue: pwa },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(PushNotificationsService);
  }

  beforeEach(() => {
    subscription$ = new BehaviorSubject<PushSubscription | null>(null);
    swPush = {
      isEnabled: true,
      subscription: subscription$,
      requestSubscription: vi.fn(async () => {
        const created = fakeSubscription();
        subscription$.next(created);
        return created;
      }),
      unsubscribe: vi.fn(async () => subscription$.next(null)),
    };
    pwa = {
      isIos: () => false,
      isStandalone: () => false,
      supportsPush: () => true,
      notificationPermission: () => 'default',
    };
  });

  afterEach(() => http.verify());

  describe('status', () => {
    it('is off when supported and not subscribed', () => {
      expect(create().status()).toBe('off');
    });

    it('is on when this device is subscribed', () => {
      subscription$.next(fakeSubscription());
      expect(create().status()).toBe('on');
    });

    it('is unsupported without Web Push APIs or with the service worker disabled', () => {
      pwa.supportsPush = () => false;
      expect(create().status()).toBe('unsupported');
      TestBed.resetTestingModule();

      pwa.supportsPush = () => true;
      swPush.isEnabled = false;
      expect(create().status()).toBe('unsupported');
    });

    it('asks to install the app on iPhone when opened in a Safari tab', () => {
      pwa.isIos = () => true;
      pwa.supportsPush = () => false;

      expect(create().status()).toBe('needs-install');
    });

    it('works on iPhone once the app is installed', () => {
      pwa.isIos = () => true;
      pwa.isStandalone = () => true;

      expect(create().status()).toBe('off');
    });

    it('is blocked when the permission was denied', () => {
      pwa.notificationPermission = () => 'denied';

      expect(create().status()).toBe('blocked');
    });
  });

  describe('enable', () => {
    it('subscribes with the server key and registers the subscription in the backend', () => {
      const service = create();
      let done = false;
      service.enable().subscribe(() => (done = true));

      const keyRequest = http.expectOne(`${BASE}/public-key`);
      expect(keyRequest.request.context.get(SKIP_ERROR_TOAST)).toBe(true);
      keyRequest.flush({ publicKey: 'server-key' });

      return vi.waitFor(() => {
        const register = http.expectOne(`${BASE}/subscriptions`);
        expect(register.request.method).toBe('POST');
        expect(register.request.body).toEqual({ endpoint: ENDPOINT, expirationTime: null, keys: { p256dh: 'p', auth: 'a' } });
        register.flush(null);
        expect(swPush.requestSubscription).toHaveBeenCalledWith({ serverPublicKey: 'server-key' });
        expect(done).toBe(true);
        expect(service.status()).toBe('on');
      });
    });

    it('becomes blocked when the user denies the permission prompt', () => {
      const service = create();
      swPush.requestSubscription.mockRejectedValueOnce(new DOMException('denied', 'NotAllowedError'));
      pwa.notificationPermission = () => 'denied';
      let error: unknown;
      service.enable().subscribe({ error: (e: unknown) => (error = e) });

      http.expectOne(`${BASE}/public-key`).flush({ publicKey: 'server-key' });

      return vi.waitFor(() => {
        expect(error).toBeInstanceOf(DOMException);
        expect(service.status()).toBe('blocked');
        http.expectNone(`${BASE}/subscriptions`);
      });
    });

    it('fails without subscribing when the backend has no push configured', () => {
      const service = create();
      let error: unknown;
      service.enable().subscribe({ error: (e: unknown) => (error = e) });

      http.expectOne(`${BASE}/public-key`).flush({ message: 'no' }, { status: 503, statusText: 'Service Unavailable' });

      expect(error).toBeInstanceOf(HttpErrorResponse);
      expect(swPush.requestSubscription).not.toHaveBeenCalled();
    });
  });

  describe('disable', () => {
    it('removes the device in the backend and unsubscribes locally', () => {
      subscription$.next(fakeSubscription());
      const service = create();
      let done = false;
      service.disable().subscribe(() => (done = true));

      const request = http.expectOne(`${BASE}/subscriptions`);
      expect(request.request.method).toBe('DELETE');
      expect(request.request.body).toEqual({ endpoint: ENDPOINT });
      request.flush(null);

      return vi.waitFor(() => {
        expect(swPush.unsubscribe).toHaveBeenCalled();
        expect(done).toBe(true);
        expect(service.status()).toBe('off');
      });
    });

    it('still unsubscribes locally if the backend call fails', () => {
      subscription$.next(fakeSubscription());
      const service = create();
      service.disable().subscribe();

      http.expectOne(`${BASE}/subscriptions`).flush(null, { status: 500, statusText: 'Server Error' });

      return vi.waitFor(() => expect(swPush.unsubscribe).toHaveBeenCalled());
    });

    it('does nothing when the device is not subscribed', () => {
      create().disable().subscribe();

      http.expectNone(`${BASE}/subscriptions`);
      expect(swPush.unsubscribe).not.toHaveBeenCalled();
    });
  });

  describe('sendTest', () => {
    it('asks the backend for a test notification', () => {
      let delivered = -1;
      create()
        .sendTest()
        .subscribe((response) => (delivered = response.delivered));

      const request = http.expectOne(`${BASE}/test`);
      expect(request.request.method).toBe('POST');
      request.flush({ delivered: 1 });

      expect(delivered).toBe(1);
    });
  });

  describe('syncExisting', () => {
    it('re-registers an existing subscription once per session', () => {
      subscription$.next(fakeSubscription());
      const service = create();

      service.syncExisting().subscribe();
      http.expectOne(`${BASE}/subscriptions`).flush(null);
      service.syncExisting().subscribe();

      http.expectNone(`${BASE}/subscriptions`);
    });

    it('does nothing without a subscription', () => {
      create().syncExisting().subscribe();

      http.expectNone(`${BASE}/subscriptions`);
    });
  });
});
