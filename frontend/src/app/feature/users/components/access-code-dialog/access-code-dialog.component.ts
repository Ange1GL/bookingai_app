import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { CopyButtonComponent } from '@/shared/components/copy-button/copy-button.component';
import { fullName, UserOneTimeCodeDto } from '../../models/user.dto';

/**
 * Shows the access code issued for a user so the admin can copy it and hand it over through their own chat.
 * The backend only returns the code once, so closing the dialog loses it (a new one can be generated).
 */
@Component({
  selector: 'app-access-code-dialog',
  imports: [DialogModule, ButtonModule, CopyButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './access-code-dialog.component.html',
})
export class AccessCodeDialogComponent {
  readonly visible = model(false);
  readonly result = input.required<UserOneTimeCodeDto | null>();

  protected readonly displayName = computed(() => {
    const user = this.result()?.user;
    return user ? fullName(user) : '';
  });
}
