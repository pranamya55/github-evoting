/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TranslatableText } from 'e-voting-libraries-ui-kit';

export const mockSortBy = (val: any) => val;
export const mockConcat = (val1: any, val2: any) => [
	...(val1 ?? []),
	...(val2 ?? []),
];
export const mockTranslateText = (text: TranslatableText) => text.DE;
