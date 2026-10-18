import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { AppointmentDto, CreateAppointmentRequestDto } from '../models/appointment.dto';
import { toLocalIso } from '../utils/calendar-date.util';

@Injectable({ providedIn: 'root' })
export class AppointmentsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/appointments`;

  /** Full detail of one appointment, including its service and price. */
  findById(id: number): Observable<AppointmentDto> {
    return this.http.get<AppointmentDto>(`${this.url}/${id}`, { context: new HttpContext().set(SKIP_LOADER, true) });
  }

  /** Books an appointment; the backend rejects overlaps, past times and blacklisted customers (409/400). */
  create(request: CreateAppointmentRequestDto): Observable<AppointmentDto> {
    return this.http.post<AppointmentDto>(this.url, request, { context: new HttpContext().set(SKIP_LOADER, true) });
  }

  /** Cancels an appointment (not a delete: it stays listed as cancelled). Returns the updated appointment. */
  cancel(id: number): Observable<AppointmentDto> {
    return this.http.post<AppointmentDto>(`${this.url}/${id}/cancel`, {}, { context: new HttpContext().set(SKIP_LOADER, true) });
  }

  /** Appointments of the authenticated user in the half-open range [from, to). */
  findByDateRange(from: Date, to: Date): Observable<AppointmentDto[]> {
    const params = new HttpParams().set('from', toLocalIso(from)).set('to', toLocalIso(to));
    // Callers render their own skeletons, so the full-screen loader stays out of period navigation.
    return this.http.get<AppointmentDto[]>(this.url, { params, context: new HttpContext().set(SKIP_LOADER, true) });
  }
}
