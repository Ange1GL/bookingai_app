import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DateTimeParts } from '../../models/date-time-field.model';
import { DateTimeFieldComponent } from './date-time-field.component';

@Component({
  imports: [ReactiveFormsModule, DateTimeFieldComponent],
  template: `<app-date-time-field inputId="when" [formControl]="control" />`,
})
class HostComponent {
  readonly control = new FormControl<Date | null>(null);
}

describe('DateTimeFieldComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  let field: DateTimeFieldComponent;

  /** The inner controls are what the date picker and the masked input write to. */
  function parts(): FormGroup<DateTimeParts> {
    return field['parts'] as FormGroup<DateTimeParts>;
  }

  beforeEach(() => {
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
    field = fixture.debugElement.children[0]?.componentInstance as DateTimeFieldComponent;
  });

  it('shows an incoming date and its time', () => {
    host.control.setValue(new Date(2026, 9, 23, 5, 3));

    expect(parts().getRawValue().time).toBe('05:03');
    expect(parts().getRawValue().date).toEqual(new Date(2026, 9, 23, 5, 3));
  });

  it('emits the combined value once the typed time is complete, with any minute', () => {
    parts().setValue({ date: new Date(2026, 9, 23), time: '17:13' });

    expect(host.control.value).toEqual(new Date(2026, 9, 23, 17, 13));
  });

  it('never emits a half-typed time', () => {
    host.control.setValue(new Date(2026, 9, 23, 9, 0));

    parts().patchValue({ time: '17:1_' });

    expect(host.control.value).toBeNull();
  });

  it('flags a time that is not real (25:70) but not an empty one', () => {
    parts().setValue({ date: new Date(2026, 9, 23), time: '25:70' });
    expect(host.control.hasError('invalidTime')).toBe(true);

    parts().setValue({ date: new Date(2026, 9, 23), time: '' });
    expect(host.control.hasError('invalidTime')).toBe(false);
  });

  it('keeps the new day when only the date changes', () => {
    parts().setValue({ date: new Date(2026, 9, 23), time: '17:13' });

    parts().patchValue({ date: new Date(2026, 9, 25) });

    expect(host.control.value).toEqual(new Date(2026, 9, 25, 17, 13));
  });

  it('disables both inputs with the form control', () => {
    host.control.disable();

    expect(parts().disabled).toBe(true);
  });
});
