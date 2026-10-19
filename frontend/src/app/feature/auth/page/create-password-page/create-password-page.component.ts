import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthCardComponent } from '@/shared/components/auth-card/auth-card.component';
import { CreatePasswordFormComponent } from '../../components/create-password-form/create-password-form.component';
import { CreatePasswordRequestDto } from '../../models/create-password-request.dto';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-create-password-page',
  imports: [AuthCardComponent, CreatePasswordFormComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './create-password-page.component.html',
})
export class CreatePasswordPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);

  protected createPassword(request: CreatePasswordRequestDto): void {
    this.loading.set(true);
    this.authService
      .createPassword(request)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/home'),
        error: () => undefined, // surfaced by errorInterceptor
      });
  }
}
