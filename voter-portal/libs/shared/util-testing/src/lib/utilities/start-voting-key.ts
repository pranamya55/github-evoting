/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { RandomString } from './random';

export const RandomStartVotingKey = () => {
	return RandomString(24);
};
