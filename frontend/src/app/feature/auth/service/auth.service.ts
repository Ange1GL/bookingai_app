import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthUserDto } from '@/core/model/auth-user.dto';
import { SessionService } from '@/core/service/session.service';
import { CreatePasswordRequestDto } from '../models/create-password-request.dto';
import { LoginRequestDto } from '../models/login-request.dto';
import { RegisterRequestDto } from '../models/register-request.dto';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);

  login(request: LoginRequestDto): Observable<AuthUserDto> {
    return this.http
      .post<AuthUserDto>(`${environment.apiBaseUrl}/api/v1/auth/login`, request)
      .pipe(tap((user) => this.session.setUser(user)));
  }

  /** Redeems the one-time code received by WhatsApp: sets the password and opens a session. */
  createPassword(request: CreatePasswordRequestDto): Observable<AuthUserDto> {
    return this.http
      .post<AuthUserDto>(`${environment.apiBaseUrl}/api/v1/auth/create-password`, request)
      .pipe(tap((user) => this.session.setUser(user)));
  }

  register(request: RegisterRequestDto): Observable<AuthUserDto> {
    return this.http
      .post<AuthUserDto>(`${environment.apiBaseUrl}/api/v1/auth/register`, request)
      .pipe(tap((user) => this.session.setUser(user)));
  }
}
