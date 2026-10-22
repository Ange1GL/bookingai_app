import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';
import { Permission } from '@/core/security/permissions';
import { SessionService } from '@/core/service/session.service';

/** Renders the host template only when the user holds any of the given permissions: `*appHasPermission="[perm]"`. */
@Directive({ selector: '[appHasPermission]' })
export class HasPermissionDirective {
  private readonly session = inject(SessionService);
  private readonly template = inject(TemplateRef<unknown>);
  private readonly container = inject(ViewContainerRef);

  readonly appHasPermission = input.required<readonly Permission[]>();

  constructor() {
    effect(() => {
      this.container.clear();
      if (this.session.hasAnyPermission(this.appHasPermission())) {
        this.container.createEmbeddedView(this.template);
      }
    });
  }
}
