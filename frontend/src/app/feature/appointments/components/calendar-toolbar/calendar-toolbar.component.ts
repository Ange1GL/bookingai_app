import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CalendarView } from '../../models/calendar.model';

interface ViewOption {
  value: CalendarView;
  label: string;
}

const VIEW_OPTIONS: ViewOption[] = [
  { value: 'month', label: 'Mes' },
  { value: 'week', label: 'Semana' },
];

@Component({
  selector: 'app-calendar-toolbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './calendar-toolbar.component.html',
})
export class CalendarToolbarComponent {
  readonly title = input.required<string>();
  readonly view = input.required<CalendarView>();

  readonly previous = output<void>();
  readonly next = output<void>();
  readonly today = output<void>();
  readonly viewChange = output<CalendarView>();

  protected readonly viewOptions = VIEW_OPTIONS;
}
