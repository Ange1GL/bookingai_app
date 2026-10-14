import { ChangeDetectionStrategy, Component } from '@angular/core';
import { LegalDocumentComponent } from '../../components/legal-document/legal-document.component';
import { PRIVACY_LAST_UPDATE, PRIVACY_SECTIONS } from '../../data/privacy-policy';

@Component({
  selector: 'app-privacy-page',
  imports: [LegalDocumentComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './privacy-page.component.html',
})
export class PrivacyPageComponent {
  protected readonly sections = PRIVACY_SECTIONS;
  protected readonly lastUpdate = PRIVACY_LAST_UPDATE;
}
