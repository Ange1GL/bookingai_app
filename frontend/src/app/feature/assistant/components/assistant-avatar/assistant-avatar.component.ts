import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-assistant-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="flex shrink-0 items-center justify-center rounded-full bg-linear-to-br from-primary-400 to-primary-700 text-white shadow-md shadow-primary-500/30"
      [class]="large() ? 'size-12 text-xl' : 'size-8 text-sm'"
      aria-hidden="true"
    >
      <i class="pi pi-sparkles"></i>
    </span>
  `,
})
export class AssistantAvatarComponent {
  readonly large = input(false);
}
