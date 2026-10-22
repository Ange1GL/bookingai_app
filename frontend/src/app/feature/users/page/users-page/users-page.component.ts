import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, linkedSignal, signal, viewChild } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { PERMISSIONS } from '@/core/security/permissions';
import { PagerComponent } from '@/shared/components/pager/pager.component';
import { HasPermissionDirective } from '@/shared/directives/has-permission.directive';
import { ResetPasswordFlowComponent, ResetTarget } from '../../components/reset-password-flow/reset-password-flow.component';
import { UserCardComponent } from '../../components/user-card/user-card.component';
import { UserDetailDialogComponent } from '../../components/user-detail-dialog/user-detail-dialog.component';
import { UserFiltersComponent } from '../../components/user-filters/user-filters.component';
import { UserFormDialogComponent } from '../../components/user-form-dialog/user-form-dialog.component';
import { CreateUserRequestDto, fullName, USER_PAGE_SIZE, UserDto, UserPageDto } from '../../models/user.dto';
import { ActiveFilter, INITIAL_USER_QUERY, UserQuery } from '../../models/user-query.model';
import { UsersService } from '../../service/users.service';

const SKELETON_ROWS = [0, 1, 2, 3];
const TOAST_LIFE_MS = 3000;

@Component({
  selector: 'app-users-page',
  imports: [
    RouterLink,
    ButtonModule,
    SkeletonModule,
    PagerComponent,
    HasPermissionDirective,
    UserCardComponent,
    UserFiltersComponent,
    UserFormDialogComponent,
    UserDetailDialogComponent,
    ResetPasswordFlowComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './users-page.component.html',
})
export class UsersPageComponent {
  private readonly usersService = inject(UsersService);
  private readonly messages = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly resetFlow = viewChild.required(ResetPasswordFlowComponent);

  protected readonly pageSize = USER_PAGE_SIZE;
  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly writePermissions = [PERMISSIONS.USER_WRITE];

  protected readonly query = signal<UserQuery>({ ...INITIAL_USER_QUERY });
  protected readonly formVisible = signal(false);
  protected readonly creating = signal(false);
  protected readonly resetVisible = signal(false);
  protected readonly detailVisible = signal(false);
  protected readonly detailUserId = signal<number | null>(null);
  protected readonly resetTarget = signal<ResetTarget | null>(null);

  private readonly resource = rxResource({
    params: () => this.query(),
    stream: ({ params }) => this.usersService.list(params),
  });

  /** Keeps the last good page on screen while the next one loads, avoiding skeleton flicker. */
  private readonly page = linkedSignal<UserPageDto | undefined, UserPageDto | undefined>({
    source: () => (this.resource.hasValue() ? this.resource.value() : undefined),
    computation: (value, previous) => value ?? previous?.value,
  });

  protected readonly loading = this.resource.isLoading;
  protected readonly failed = computed(() => this.resource.error() !== undefined);
  protected readonly users = computed(() => this.page()?.content ?? []);
  protected readonly totalRecords = computed(() => this.page()?.totalElements ?? 0);
  protected readonly hasFilters = computed(() => this.query().search !== '' || this.query().status !== 'all');
  protected readonly totalLabel = computed(() => {
    const total = this.totalRecords();
    return total === 1 ? '1 usuario' : `${total} usuarios`;
  });

  protected setSearch(search: string): void {
    this.query.update((query) => ({ ...query, search, page: 0 }));
  }

  protected setStatus(status: ActiveFilter): void {
    this.query.update((query) => ({ ...query, status, page: 0 }));
  }

  protected changePage(page: number): void {
    this.query.update((query) => ({ ...query, page }));
  }

  protected reload(): void {
    this.resource.reload();
  }

  protected create(request: CreateUserRequestDto): void {
    this.creating.set(true);
    this.usersService
      .create(request)
      .pipe(
        finalize(() => this.creating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (created) => {
          this.formVisible.set(false);
          this.messages.add({ severity: 'success', summary: 'Listo', detail: `${fullName(created.user)} creado`, life: TOAST_LIFE_MS });
          this.resetFlow().show(created);
          this.reload();
        },
        // errorInterceptor already shows the toast; keep the dialog open so the user can retry.
        error: () => undefined,
      });
  }

  protected showDetail(user: UserDto): void {
    this.detailUserId.set(user.id);
    this.detailVisible.set(true);
  }

  protected askReset(user: UserDto): void {
    this.resetTarget.set({ id: user.id, displayName: fullName(user) });
    this.resetVisible.set(true);
  }
}
