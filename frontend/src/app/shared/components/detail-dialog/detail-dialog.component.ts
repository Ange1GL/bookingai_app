import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SkeletonModule } from 'primeng/skeleton';
import { DetailField } from '../../models/detail-field.model';

const SKELETON_ROWS = [0, 1, 2, 3];

/**
 * Read-only detail modal: a title, a list of labelled fields and optional extra content (`ng-content`).
 * It also renders the loading and error states, so each feature only fetches the data and maps it to fields.
 */
@Component({
  selector: 'app-detail-dialog',
  imports: [DialogModule, ButtonModule, SkeletonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './detail-dialog.component.html',
})
export class DetailDialogComponent {
  readonly visible = model(false);
  readonly header = input.required<string>();
  readonly title = input('');
  /** Optional avatar text (e.g. initials) shown next to the title. */
  readonly initials = input('');
  readonly fields = input<DetailField[]>([]);
  readonly loading = input(false);
  readonly failed = input(false);
  readonly retry = output<void>();

  protected readonly skeletonRows = SKELETON_ROWS;
}
