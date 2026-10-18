import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { CreateCustomerRequestDto } from '@/shared/models/customer-input.model';
import { CUSTOMER_SUGGESTIONS_SIZE, CustomerOptionDto, CustomerOptionsPageDto, ServiceOptionDto } from '../models/booking-options.model';
import { toCustomerSearchParams } from '../utils/customer-search.util';

const ownLoading = () => new HttpContext().set(SKIP_LOADER, true);

/**
 * Read-only lookups the booking form needs. The appointments feature owns these thin calls
 * instead of importing the customers / price-catalog services (feature → feature is forbidden).
 */
@Injectable({ providedIn: 'root' })
export class BookingLookupService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/api/v1`;

  searchCustomers(term: string): Observable<CustomerOptionDto[]> {
    let params = new HttpParams().set('page', 0).set('size', CUSTOMER_SUGGESTIONS_SIZE);
    const { name, phone } = toCustomerSearchParams(term);
    if (name) {
      params = params.set('name', name);
    }
    if (phone) {
      params = params.set('phone', phone);
    }
    return this.http
      .get<CustomerOptionsPageDto>(`${this.apiUrl}/customers`, { params, context: ownLoading() })
      .pipe(map((page) => page.content));
  }

  /** Idempotent by phone: returns the existing customer when it was already registered. */
  createCustomer(request: CreateCustomerRequestDto): Observable<CustomerOptionDto> {
    return this.http
      .post<Omit<CustomerOptionDto, 'blacklisted'>>(`${this.apiUrl}/customers`, request, { context: ownLoading() })
      .pipe(map((customer) => ({ ...customer, blacklisted: false })));
  }

  listServices():Observable<ServiceOptionDto[]> {
    return this.http.get<ServiceOptionDto[]>(`${this.apiUrl}/price-catalog`, { context: ownLoading() });
  }
}
