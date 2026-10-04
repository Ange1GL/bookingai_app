import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BrandLogoComponent } from '../brand-logo/brand-logo.component';

@Component({
  selector: 'app-auth-card',
  imports: [BrandLogoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main
      class="flex min-h-screen items-center justify-center bg-linear-to-br from-primary-50 via-white to-primary-100 p-4 dark:from-primary-950 dark:via-surface-950 dark:to-primary-900"
    >
      <section
        class="flex w-full max-w-md flex-col gap-6 rounded-2xl border border-primary-100/70 bg-white/80 p-8 shadow-xl shadow-primary-500/10 backdrop-blur dark:border-primary-900 dark:bg-surface-900/80"
      >
        <app-brand-logo />
        <ng-content />
      </section>
    </main>
  `,
})
export class AuthCardComponent {}
