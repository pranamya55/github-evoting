/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { RandomString } from './random';

export const RandomElectionEventId = () => {
	return RandomString(32, '0123456789ABCDEF');
};
