import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PERMISSIONS } from '@/core/security/permissions';
import { IconButtonComponent } from '@/shared/components/icon-button/icon-button.component';
import { HasPermissionDirective } from '@/shared/directives/has-permission.directive';
import { PasswordRecoveryRequestDto } from '../../models/password-recovery-request.dto';
import { fullName } from '../../models/user.dto';

const REQUESTED_AT_FORMAT = new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' });

@Component({
  selector: 'app-recovery-request-card',
  imports: [IconButtonComponent, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './recovery-request-card.component.html',
})
export class RecoveryRequestCardComponent {
  readonly request = input.required<PasswordRecoveryRequestDto>();
  readonly attendRequested = output<PasswordRecoveryRequestDto>();

  protected readonly writePermissions = [PERMISSIONS.USER_WRITE];
  protected readonly displayName = computed(() => fullName(this.request()));
  protected readonly attendTooltip = computed(() => `Generar un nuevo código de acceso para ${this.displayName()}`);
  protected readonly requestedAt = computed(() => REQUESTED_AT_FORMAT.format(new Date(this.request().requestedAt)));
}
