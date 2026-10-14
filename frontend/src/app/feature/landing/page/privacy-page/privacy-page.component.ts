import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CONTACT_EMAIL, LOGIN_PATH } from '../../data/contact';
import { PRIVACY_LAST_UPDATE, PRIVACY_SECTIONS } from '../../data/privacy-policy';

@Component({
  selector: 'app-privacy-page',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './privacy-page.component.html',
})
export class PrivacyPageComponent {
  protected readonly sections = PRIVACY_SECTIONS;
  protected readonly lastUpdate = PRIVACY_LAST_UPDATE;
  protected readonly loginPath = LOGIN_PATH;
  protected readonly contactEmail = CONTACT_EMAIL;
  protected readonly contactHref = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent('Privacidad aglacita')}`;
}
