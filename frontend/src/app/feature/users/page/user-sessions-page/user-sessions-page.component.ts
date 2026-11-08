import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { ConfirmDialogComponent } from '@/shared/components/confirm-dialog/confirm-dialog.component';
import { SessionCardComponent } from '../../components/session-card/session-card.component';
import { fullName } from '../../models/user.dto';
import { UserSessionDto } from '../../models/user-session.dto';
import { UserSessionsService } from '../../service/user-sessions.service';
import { UsersService } from '../../service/users.service';

const SKELETON_ROWS = [0, 1];
const TOAST_LIFE_MS = 3000;
const USER_ID_PARAM = 'id';

@Component({
  selector: 'app-user-sessions-page',
  imports: [RouterLink, ButtonModule, SkeletonModule, ConfirmDialogComponent, SessionCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-sessions-page.component.html',
})
export class UserSessionsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly usersService = inject(UsersService);
  private readonly sessionsService = inject(UserSessionsService);
  private readonly messages = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly userId = Number(this.route.snapshot.paramMap.get(USER_ID_PARAM));
  protected readonly skeletonRows = SKELETON_ROWS;

  protected readonly closeOneVisible = signal(false);
  protected readonly closeAllVisible = signal(false);
  protected readonly target = signal<UserSessionDto | null>(null);
  protected readonly busy = signal(false);

  private readonly userResource = rxResource({ stream: () => this.usersService.get(this.userId) });
  private readonly sessionsResource = rxResource({ stream: () => this.sessionsService.list(this.userId) });

  protected readonly loading = this.sessionsResource.isLoading;
  protected readonly failed = computed(() => this.sessionsResource.error() !== undefined);
  protected readonly sessions = computed(() => (this.sessionsResource.hasValue() ? this.sessionsResource.value() : []));
  protected readonly displayName = computed(() => (this.userResource.hasValue() ? fullName(this.userResource.value()) : ''));
  protected readonly totalLabel = computed(() => {
    const total = this.sessions().length;
    return total === 1 ? '1 sesión activa' : `${total} sesiones activas`;
  });
  protected readonly closeOneMessage = computed(
    () =>
      'Se cerrará esta sesión y no podrá renovarse. El acceso que ya tiene abierto puede seguir funcionando unos minutos (máximo 15) ' +
      'hasta que venza.',
  );
  protected readonly closeAllMessage = computed(
    () => `Se cerrarán las ${this.sessions().length} sesiones de ${this.displayName() || 'este usuario'} y quedarán sin acceso de inmediato.`,
  );

  protected reload(): void {
    this.sessionsResource.reload();
  }

  protected askCloseOne(session: UserSessionDto): void {
    this.target.set(session);
    this.closeOneVisible.set(true);
  }

  protected confirmCloseOne(): void {
    const session = this.target();
    if (session) {
      this.run(this.sessionsService.revoke(this.userId, session.id), this.closeOneVisible, 'Sesión cerrada');
    }
  }

  protected confirmCloseAll(): void {
    this.run(this.sessionsService.revokeAll(this.userId), this.closeAllVisible, 'Todas las sesiones fueron cerradas');
  }

  private run(request: Observable<void>, dialogVisible: { set(value: boolean): void }, successDetail: string): void {
    this.busy.set(true);
    request
      .pipe(
        finalize(() => {
          this.busy.set(false);
          dialogVisible.set(false);
          // Reload on failure too: a 404 means the session was already closed, so the list was stale.
          this.reload();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.messages.add({ severity: 'success', summary: 'Listo', detail: successDetail, life: TOAST_LIFE_MS }),
        // errorInterceptor already shows the toast.
        error: () => undefined,
      });
  }
}
