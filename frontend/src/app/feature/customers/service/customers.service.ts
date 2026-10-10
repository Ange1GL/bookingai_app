import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { CreateCustomerRequestDto, CustomerDto, CustomerPageDto, PAGE_SIZE } from '../models/customer.dto';
import { CustomerQuery, toSearchParams } from '../models/customer-query.model';

// Every caller renders its own loading state (skeletons, button spinners).
const ownLoading = () => new HttpContext().set(SKIP_LOADER, true);

@Injectable({ providedIn: 'root' })
export class CustomersService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/customers`;

  list(query: CustomerQuery): Observable<CustomerPageDto> {
    let params = new HttpParams().set('page', query.page).set('size', PAGE_SIZE);
    const { name, phone } = toSearchParams(query.search);
    if (name) {
      params = params.set('name', name);
    }
    if (phone) {
      params = params.set('phone', phone);
    }
    if (query.blacklist === 'blacklisted') {
      params = params.set('blacklisted', true);
    }
    return this.http.get<CustomerPageDto>(this.url, { params, context: ownLoading() });
  }

  /** Idempotent by phone: returns the existing customer when it was already registered. */
  create(request: CreateCustomerRequestDto): Observable<CustomerDto> {
    return this.http.post<CustomerDto>(this.url, request, { context: ownLoading() });
  }

  blacklist(id: number, reason: string | null): Observable<void> {
    return this.http.post<void>(`${this.url}/${id}/blacklist`, reason ? { reason } : {}, { context: ownLoading() });
  }

  removeFromBlacklist(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}/blacklist`, { context: ownLoading() });
  }
}
