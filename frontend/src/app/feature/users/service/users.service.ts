import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { CreateUserRequestDto, USER_PAGE_SIZE, UserDto, UserOneTimeCodeDto, UserPageDto } from '../models/user.dto';
import { toActiveParam, UserQuery } from '../models/user-query.model';

// Every caller renders its own loading state (skeletons, button spinners).
const ownLoading = () => new HttpContext().set(SKIP_LOADER, true);

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/users`;

  list(query: UserQuery): Observable<UserPageDto> {
    let params = new HttpParams().set('page', query.page).set('size', USER_PAGE_SIZE);
    if (query.search) {
      params = params.set('q', query.search);
    }
    const active = toActiveParam(query.status);
    if (active !== null) {
      params = params.set('active', active);
    }
    return this.http.get<UserPageDto>(this.url, { params, context: ownLoading() });
  }

  get(id: number): Observable<UserDto> {
    return this.http.get<UserDto>(`${this.url}/${id}`, { context: ownLoading() });
  }

  /** The response carries the one-time access code when delivery is manual. */
  create(request: CreateUserRequestDto): Observable<UserOneTimeCodeDto> {
    return this.http.post<UserOneTimeCodeDto>(this.url, request, { context: ownLoading() });
  }

  /** Wipes the current password, closes the sessions and issues a new one-time access code. */
  resetPassword(id: number): Observable<UserOneTimeCodeDto> {
    return this.http.post<UserOneTimeCodeDto>(`${this.url}/${id}/reset-password`, null, { context: ownLoading() });
  }
}
