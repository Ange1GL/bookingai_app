import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, model, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ConfirmDialogComponent } from '@/shared/components/confirm-dialog/confirm-dialog.component';
import { UserOneTimeCodeDto } from '../../models/user.dto';
import { UsersService } from '../../service/users.service';
import { AccessCodeDialogComponent } from '../access-code-dialog/access-code-dialog.component';

/** Who gets a new access code. */
export interface ResetTarget {
  id: number;
  displayName: string;
}

/** Confirm → issue a new access code → show it for copying. Shared by the user list and the recovery inbox. */
@Component({
  selector: 'app-reset-password-flow',
  imports: [ConfirmDialogComponent, AccessCodeDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reset-password-flow.component.html',
})
export class ResetPasswordFlowComponent {
  private readonly usersService = inject(UsersService);
  private readonly destroyRef = inject(DestroyRef);

  readonly target = input.required<ResetTarget | null>();
  readonly confirmVisible = model(false);
  /** Emits once the new code was issued, so the parent can refresh its list. */
  readonly issued = output<UserOneTimeCodeDto>();

  protected readonly busy = signal(false);
  protected readonly codeVisible = signal(false);
  protected readonly result = signal<UserOneTimeCodeDto | null>(null);
  protected readonly message = computed(
    () =>
      `Se borrará la contraseña actual de ${this.target()?.displayName ?? 'este usuario'}, se cerrarán sus sesiones y ` +
      'se generará un código de acceso nuevo para que cree otra.',
  );

  protected confirm(): void {
    const target = this.target();
    if (!target || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.usersService
      .resetPassword(target.id)
      .pipe(
        finalize(() => this.busy.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (issued) => {
          this.confirmVisible.set(false);
          this.result.set(issued);
          this.codeVisible.set(true);
          this.issued.emit(issued);
        },
        // errorInterceptor already shows the toast; keep the dialog open so the user can retry.
        error: () => undefined,
      });
  }

  /** Lets the parent show a code it got elsewhere (e.g. right after creating a user). */
  show(issued: UserOneTimeCodeDto): void {
    this.result.set(issued);
    this.codeVisible.set(true);
  }
}
