/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
type ObjectKey = string | number | symbol;

type Join<
	L extends ObjectKey | undefined,
	R extends ObjectKey | undefined,
> = L extends string | number
	? R extends string | number
		? `${L}.${R}`
		: L
	: R extends string | number
		? R
		: undefined;

type Union<
	L extends unknown | undefined,
	R extends unknown | undefined,
> = L extends undefined
	? R extends undefined
		? undefined
		: R
	: R extends undefined
		? L
		: L | R;

export type Paths<
	T extends object,
	Prev extends ObjectKey | undefined = undefined,
	Path extends ObjectKey | undefined = undefined,
> = {
	[K in keyof Required<T>]: Extract<T[K], object> extends unknown[] | never
		? Union<Union<Prev, Path>, Join<Path, K>>
		: Paths<Extract<T[K], object>, Union<Prev, Path>, Join<Path, K>>;
}[keyof T];
