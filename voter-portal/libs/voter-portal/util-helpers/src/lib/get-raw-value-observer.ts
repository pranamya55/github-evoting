/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { map, Observable, of } from 'rxjs';
import { FormGroup } from '@angular/forms';

/**
 * Returns an observable emitting up-to-date form controls whenever their value changes.
 */
export function getRawValueObservable(
	formGroup: FormGroup,
	formControlName: string,
): Observable<any | undefined> {
	const formControl = formGroup.get(formControlName);

	return (
		formControl?.valueChanges.pipe(map(() => formControl.getRawValue())) ??
		of(undefined)
	);
}
