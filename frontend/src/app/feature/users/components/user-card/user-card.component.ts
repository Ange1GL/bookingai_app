import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PERMISSIONS } from '@/core/security/permissions';
import { IconButtonComponent } from '@/shared/components/icon-button/icon-button.component';
import { HasPermissionDirective } from '@/shared/directives/has-permission.directive';
import { fullName, UserDto } from '../../models/user.dto';

const INITIALS_COUNT = 2;

@Component({
  selector: 'app-user-card',
  imports: [IconButtonComponent, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-card.component.html',
})
export class UserCardComponent {
  readonly user = input.required<UserDto>();
  readonly busy = input(false);
  readonly detailRequested = output<UserDto>();
  readonly resetRequested = output<UserDto>();

  protected readonly writePermissions = [PERMISSIONS.USER_WRITE];
  protected readonly displayName = computed(() => fullName(this.user()));
  protected readonly detailTooltip = computed(() => `Ver detalle de ${this.displayName()}`);
  protected readonly resetTooltip = computed(() => `Generar un nuevo código de acceso para ${this.displayName()}`);
  protected readonly initials = computed(() =>
    this.displayName()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, INITIALS_COUNT)
      .map((word) => word[0]?.toUpperCase())
      .join(''),
  );
}
