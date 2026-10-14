import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CONTACT_EMAIL, LOGIN_PATH } from '../../data/contact';
import { LegalSection } from '../../data/legal-section';

/** Public legal text (privacy policy, terms of service) with the landing's header and contact block. */
@Component({
  selector: 'app-legal-document',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './legal-document.component.html',
})
export class LegalDocumentComponent {
  readonly heading = input.required<string>();
  readonly lastUpdate = input.required<string>();
  readonly sections = input.required<LegalSection[]>();
  readonly contactTitle = input.required<string>();
  readonly contactSubject = input.required<string>();

  protected readonly loginPath = LOGIN_PATH;
  protected readonly contactEmail = CONTACT_EMAIL;
  protected readonly contactHref = computed(
    () => `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(this.contactSubject())}`,
  );
}
