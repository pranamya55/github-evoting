/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export function RandomInt(max = 100, min = 0): number {
	if (min >= max)
		throw new Error(
			'[RandomInt] provided minimum is greater than or equal to provided maximum',
		);

	const randomInt =
		window.crypto.getRandomValues(new Uint8Array(1))[0] / Math.pow(2, 8);
	return Math.floor(randomInt * (max - min)) + min;
}
