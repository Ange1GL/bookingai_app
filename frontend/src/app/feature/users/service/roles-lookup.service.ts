import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { RoleDto, RolePageDto } from '../models/role.dto';

const ROLES_LOOKUP_SIZE = 50;

/** Active roles available when creating a user. */
@Injectable({ providedIn: 'root' })
export class RolesLookupService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/roles`;

  listActive(): Observable<RoleDto[]> {
    const params = new HttpParams().set('active', true).set('size', ROLES_LOOKUP_SIZE);
    return this.http
      .get<RolePageDto>(this.url, { params, context: new HttpContext().set(SKIP_LOADER, true) })
      .pipe(map((page) => page.content));
  }
}
