import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { UserSessionsService } from './user-sessions.service';

const URL = `${environment.apiBaseUrl}/api/v1/users`;

describe('UserSessionsService', () => {
  let service: UserSessionsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UserSessionsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the sessions of a user', () => {
    service.list(7).subscribe();

    const req = http.expectOne(`${URL}/7/sessions`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('revokes one session', () => {
    service.revoke(7, 42).subscribe();

    const req = http.expectOne(`${URL}/7/sessions/42`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('revokes every session through invalid-session', () => {
    service.revokeAll(7).subscribe();

    const req = http.expectOne(`${URL}/7/invalid-session`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
