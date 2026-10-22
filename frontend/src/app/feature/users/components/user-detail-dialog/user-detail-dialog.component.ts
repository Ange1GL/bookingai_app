import { ChangeDetectionStrategy, Component, computed, inject, input, model } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { DetailDialogComponent } from '@/shared/components/detail-dialog/detail-dialog.component';
import { DetailField } from '@/shared/models/detail-field.model';
import { fullName } from '../../models/user.dto';
import { UsersService } from '../../service/users.service';

const INITIALS_COUNT = 2;

/** Loads a user by id (only while visible) and shows it in the shared detail dialog. */
@Component({
  selector: 'app-user-detail-dialog',
  imports: [DetailDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-detail-dialog.component.html',
})
export class UserDetailDialogComponent {
  private readonly usersService = inject(UsersService);

  readonly visible = model(false);
  readonly userId = input.required<number | null>();

  private readonly resource = rxResource({
    params: () => (this.visible() ? this.userId() : null),
    stream: ({ params }) => (params === null ? of(undefined) : this.usersService.get(params)),
  });

  protected readonly loading = this.resource.isLoading;
  protected readonly failed = computed(() => this.resource.error() !== undefined);
  protected readonly user = computed(() => (this.resource.hasValue() ? this.resource.value() : undefined));
  protected readonly title = computed(() => {
    const current = this.user();
    return current ? fullName(current) : '';
  });
  protected readonly initials = computed(() =>
    this.title()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, INITIALS_COUNT)
      .map((word) => word[0]?.toUpperCase())
      .join(''),
  );
  protected readonly roles = computed(() => this.user()?.roles ?? []);
  protected readonly fields = computed<DetailField[]>(() => {
    const current = this.user();
    if (!current) {
      return [];
    }
    return [
      { label: 'Usuario', value: current.username, icon: 'pi-user' },
      { label: 'Teléfono', value: current.phone, icon: 'pi-phone' },
      { label: 'Correo', value: current.email, icon: 'pi-envelope' },
      { label: 'Estado', value: current.active ? 'Activo' : 'Inactivo', icon: 'pi-power-off' },
      { label: 'Contraseña', value: current.passwordSet ? 'Configurada' : 'Pendiente de crear', icon: 'pi-lock' },
    ];
  });

  protected retry(): void {
    this.resource.reload();
  }
}
