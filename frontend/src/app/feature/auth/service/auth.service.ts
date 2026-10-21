import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthUserDto } from '@/core/model/auth-user.dto';
import { SessionService } from '@/core/service/session.service';
import { CreatePasswordRequestDto } from '../models/create-password-request.dto';
import { LoginRequestDto } from '../models/login-request.dto';
import { PasswordRecoveryAcceptedDto } from '../models/password-recovery-accepted.dto';
import { RegisterRequestDto } from '../models/register-request.dto';
import { VerifyCodeRequestDto, VerifyCodeResponseDto } from '../models/verify-code.dto';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);

  login(request: LoginRequestDto): Observable<AuthUserDto> {
    return this.http
      .post<AuthUserDto>(`${environment.apiBaseUrl}/api/v1/auth/login`, request)
      .pipe(tap((user) => this.session.setUser(user)));
  }

  /** Step 1: checks username + one-time code without spending it and returns a short-lived signed ticket. */
  verifyCode(request: VerifyCodeRequestDto): Observable<VerifyCodeResponseDto> {
    return this.http.post<VerifyCodeResponseDto>(`${environment.apiBaseUrl}/api/v1/auth/verify-code`, request);
  }

  /** Step 2: redeems the one-time code with the step 1 ticket: sets the password and opens a session. */
  createPassword(request: CreatePasswordRequestDto): Observable<AuthUserDto> {
    return this.http
      .post<AuthUserDto>(`${environment.apiBaseUrl}/api/v1/auth/create-password`, request)
      .pipe(tap((user) => this.session.setUser(user)));
  }

  /** Public: asks the admin to reset the password. The response is the same whether or not the user exists. */
  requestPasswordRecovery(username: string): Observable<PasswordRecoveryAcceptedDto> {
    return this.http.post<PasswordRecoveryAcceptedDto>(`${environment.apiBaseUrl}/api/v1/auth/password-recovery`, { username });
  }

  register(request: RegisterRequestDto): Observable<AuthUserDto> {
    return this.http
      .post<AuthUserDto>(`${environment.apiBaseUrl}/api/v1/auth/register`, request)
      .pipe(tap((user) => this.session.setUser(user)));
  }
}
