/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

export function CryptoRandom() {
	return crypto.getRandomValues(new Uint8Array(1))[0] / Math.pow(2, 8);
}

export function RandomInt(max = 100, min = 0): number {
	return Math.floor(CryptoRandom() * (max - min)) + min;
}

export function RandomItem<T>(
	array: T[] = [],
	condition?: (item: T) => boolean,
): T {
	const positiveRandomIndex = Math.max(RandomInt(array.length), 0);
	const randomItem = array[positiveRandomIndex];

	// If the condition is not met, return another random item
	if (condition instanceof Function && !condition(randomItem)) {
		return RandomItem(array, condition);
	}

	return randomItem;
}

export function RandomItemOrUndefined<T>(array: T[] = []): T | undefined {
	return RandomBetween(RandomItem(array), undefined, 1 / (array.length + 1));
}

export function RandomKey<T extends object>(object: T): keyof T {
	return RandomItem(Object.keys(object) as Array<keyof T>);
}

export function RandomBetween<T1, T2>(
	option1: T1,
	option2: T2,
	option2Probability = 0.5,
): T1 | T2 {
	return CryptoRandom() > option2Probability ? option1 : option2;
}

export function RandomArray<T>(
	mapfn: (index: number) => T,
	maxLength?: number,
	minLength = 1,
): T[] {
	return Array.from({ length: RandomInt(maxLength, minLength) }, (_, i) =>
		mapfn(i),
	);
}

export function RandomDate(
	maxDate = new Date(),
	minDate = new Date(1970, 0, 1),
): Date {
	return new Date(
		minDate.getTime() +
			CryptoRandom() * (maxDate.getTime() - minDate.getTime()),
	);
}

export function RandomDateStruct(
	maxDate?: Date,
	minDate?: Date,
): NgbDateStruct {
	const date = RandomDate(maxDate, minDate);
	return {
		day: date.getDate(),
		month: date.getMonth() + 1,
		year: date.getFullYear(),
	};
}

export function RandomString(
	length: number = 10,
	alphabet = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz',
): string {
	if (!alphabet) {
		throw new Error(
			'RandomString needs an alphabet with at least one character.',
		);
	}

	return Array.from(
		{ length },
		() => alphabet[RandomInt(alphabet.length)],
	).join('');
}
