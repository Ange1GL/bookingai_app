import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';

const DEBOUNCE_MS = 300;

/** Text box that emits the trimmed value only after the user pauses typing. */
@Component({
  selector: 'app-search-input',
  imports: [ReactiveFormsModule, InputTextModule, IconFieldModule, InputIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './search-input.component.html',
})
export class SearchInputComponent {
  readonly placeholder = input('Buscar…');
  readonly label = input('Buscar');
  readonly maxLength = input<number | null>(null);
  readonly searched = output<string>();

  protected readonly control = new FormControl('', { nonNullable: true });

  constructor() {
    this.control.valueChanges
      .pipe(
        map((value) => value.trim()),
        debounceTime(DEBOUNCE_MS),
        distinctUntilChanged(),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((value) => this.searched.emit(value));
  }
}
