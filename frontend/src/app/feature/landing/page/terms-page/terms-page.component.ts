import { ChangeDetectionStrategy, Component } from '@angular/core';
import { LegalDocumentComponent } from '../../components/legal-document/legal-document.component';
import { TERMS_LAST_UPDATE, TERMS_SECTIONS } from '../../data/terms-of-service';

@Component({
  selector: 'app-terms-page',
  imports: [LegalDocumentComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './terms-page.component.html',
})
export class TermsPageComponent {
  protected readonly sections = TERMS_SECTIONS;
  protected readonly lastUpdate = TERMS_LAST_UPDATE;
}
