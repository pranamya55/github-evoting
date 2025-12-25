/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */


/**
 * The start voting key alphabet. The alphabet corresponds to the Base32 lowercase version excluding padding "=" of "Table 3: The Base 32 Alphabet"
 * from RFC3548. Moreover, the letters "l" and "o" are replaced by "8" and "9".
 * @type {string[]} SVK_ALPHABET
 */
export const SVK_ALPHABET: string[] = [
	String.fromCharCode(parseInt('0032', 16)), // 2 (U+0032) -> 011
	String.fromCharCode(parseInt('0033', 16)), // 3 (U+0033) -> 012
	String.fromCharCode(parseInt('0034', 16)), // 4 (U+0034) -> 013
	String.fromCharCode(parseInt('0035', 16)), // 5 (U+0035) -> 014
	String.fromCharCode(parseInt('0036', 16)), // 6 (U+0036) -> 015
	String.fromCharCode(parseInt('0037', 16)), // 7 (U+0037) -> 016
	String.fromCharCode(parseInt('0038', 16)), // 8 (U+0038) -> 017
	String.fromCharCode(parseInt('0039', 16)), // 9 (U+0039) -> 018

	String.fromCharCode(parseInt('0061', 16)), // a (U+0061) -> 045
	String.fromCharCode(parseInt('0062', 16)), // b (U+0062) -> 046
	String.fromCharCode(parseInt('0063', 16)), // c (U+0063) -> 047
	String.fromCharCode(parseInt('0064', 16)), // d (U+0064) -> 048
	String.fromCharCode(parseInt('0065', 16)), // e (U+0065) -> 049
	String.fromCharCode(parseInt('0066', 16)), // f (U+0066) -> 050
	String.fromCharCode(parseInt('0067', 16)), // g (U+0067) -> 051
	String.fromCharCode(parseInt('0068', 16)), // h (U+0068) -> 052
	String.fromCharCode(parseInt('0069', 16)), // i (U+0069) -> 053
	String.fromCharCode(parseInt('006A', 16)), // j (U+006A) -> 054
	String.fromCharCode(parseInt('006B', 16)), // k (U+006B) -> 055

	String.fromCharCode(parseInt('006D', 16)), // m (U+006D) -> 057
	String.fromCharCode(parseInt('006E', 16)), // n (U+006E) -> 058

	String.fromCharCode(parseInt('0070', 16)), // p (U+0070) -> 060
	String.fromCharCode(parseInt('0071', 16)), // q (U+0071) -> 061
	String.fromCharCode(parseInt('0072', 16)), // r (U+0072) -> 062
	String.fromCharCode(parseInt('0073', 16)), // s (U+0073) -> 063
	String.fromCharCode(parseInt('0074', 16)), // t (U+0074) -> 064
	String.fromCharCode(parseInt('0075', 16)), // u (U+0075) -> 065
	String.fromCharCode(parseInt('0076', 16)), // v (U+0076) -> 066
	String.fromCharCode(parseInt('0077', 16)), // w (U+0077) -> 067
	String.fromCharCode(parseInt('0078', 16)), // x (U+0078) -> 068
	String.fromCharCode(parseInt('0079', 16)), // y (U+0079) -> 069
	String.fromCharCode(parseInt('007A', 16))  // z (U+007A) -> 070
];

