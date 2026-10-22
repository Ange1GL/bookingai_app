import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { RecoveryRequestCardComponent } from '../../components/recovery-request-card/recovery-request-card.component';
import { ResetPasswordFlowComponent, ResetTarget } from '../../components/reset-password-flow/reset-password-flow.component';
import { PasswordRecoveryRequestDto } from '../../models/password-recovery-request.dto';
import { fullName } from '../../models/user.dto';
import { PasswordRecoveryRequestsService } from '../../service/password-recovery-requests.service';

const SKELETON_ROWS = [0, 1, 2];

@Component({
  selector: 'app-recovery-requests-page',
  imports: [RouterLink, ButtonModule, SkeletonModule, RecoveryRequestCardComponent, ResetPasswordFlowComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './recovery-requests-page.component.html',
})
export class RecoveryRequestsPageComponent {
  private readonly requestsService = inject(PasswordRecoveryRequestsService);

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly resetVisible = signal(false);
  protected readonly resetTarget = signal<ResetTarget | null>(null);

  private readonly resource = rxResource({ stream: () => this.requestsService.listPending() });

  protected readonly loading = this.resource.isLoading;
  protected readonly failed = computed(() => this.resource.error() !== undefined);
  protected readonly requests = computed(() => (this.resource.hasValue() ? this.resource.value() : []));
  protected readonly totalLabel = computed(() => {
    const total = this.requests().length;
    return total === 1 ? '1 solicitud pendiente' : `${total} solicitudes pendientes`;
  });

  protected reload(): void {
    this.resource.reload();
  }

  protected attend(request: PasswordRecoveryRequestDto): void {
    this.resetTarget.set({ id: request.userId, displayName: fullName(request) });
    this.resetVisible.set(true);
  }
}
