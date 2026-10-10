import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-assistant-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './assistant-avatar.component.html',
})
export class AssistantAvatarComponent {
  readonly large = input(false);
}
