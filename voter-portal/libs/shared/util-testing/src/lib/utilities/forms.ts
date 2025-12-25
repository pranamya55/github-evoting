/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { FormArray, FormControl } from '@angular/forms';

export const MockFormArray = (length: number): FormArray => {
	return new FormArray<any>(Array.from({ length }, () => new FormControl()));
};
