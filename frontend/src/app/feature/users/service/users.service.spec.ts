import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { INITIAL_USER_QUERY } from '../models/user-query.model';
import { UsersService } from './users.service';

const URL = `${environment.apiBaseUrl}/api/v1/users`;

describe('UsersService', () => {
  let service: UsersService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UsersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists without optional filters by default', () => {
    service.list(INITIAL_USER_QUERY).subscribe();

    const req = http.expectOne((r) => r.url === URL);
    expect(req.request.params.get('q')).toBeNull();
    expect(req.request.params.get('active')).toBeNull();
    expect(req.request.params.get('page')).toBe('0');
    req.flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('sends the search text and the active filter', () => {
    service.list({ search: 'maria', status: 'inactive', page: 2 }).subscribe();

    const req = http.expectOne((r) => r.url === URL);
    expect(req.request.params.get('q')).toBe('maria');
    expect(req.request.params.get('active')).toBe('false');
    expect(req.request.params.get('page')).toBe('2');
    req.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('posts the new user', () => {
    const body = { username: 'maria', name: 'María', lastName: 'López', email: null, phone: '5512345678', roleIds: [1] };
    service.create(body).subscribe();

    const req = http.expectOne(URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('resets a password through the user id', () => {
    service.resetPassword(7).subscribe();

    const req = http.expectOne(`${URL}/7/reset-password`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
