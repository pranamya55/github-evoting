/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { FormArray, FormControl, FormGroup } from '@angular/forms';

type Defined<T> = Exclude<T, undefined>;

export type FormArrayFrom<T> = T extends object
	? FormArray<FormGroupFrom<T>>
	: never;

export type FormGroupFrom<T extends object> = FormGroup<{
	[K in keyof T]: Defined<T[K]> extends any[]
		? FormArrayFrom<Defined<T[K]>[number]>
		: Defined<T[K]> extends object
			? FormGroupFrom<Defined<T[K]>>
			: FormControl<T[K]>;
}>;
