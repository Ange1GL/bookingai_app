import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { PriceCatalogItemDto, PriceCatalogRequestDto } from '../models/price-catalog.dto';

// Every caller renders its own loading state (skeletons, button spinners).
const ownLoading = () => new HttpContext().set(SKIP_LOADER, true);

@Injectable({ providedIn: 'root' })
export class PriceCatalogService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/price-catalog`;

  list(): Observable<PriceCatalogItemDto[]> {
    return this.http.get<PriceCatalogItemDto[]>(this.url, { context: ownLoading() });
  }

  create(request: PriceCatalogRequestDto): Observable<PriceCatalogItemDto> {
    return this.http.post<PriceCatalogItemDto>(this.url, request, { context: ownLoading() });
  }

  update(id: number, request: PriceCatalogRequestDto): Observable<PriceCatalogItemDto> {
    return this.http.put<PriceCatalogItemDto>(`${this.url}/${id}`, request, { context: ownLoading() });
  }

  /** Soft delete on the backend: existing appointments keep showing the service. */
  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`, { context: ownLoading() });
  }
}
