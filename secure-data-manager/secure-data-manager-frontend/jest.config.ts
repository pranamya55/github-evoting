/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {getJestProjectsAsync} from '@nx/jest';

export default async () => ({
	projects: await getJestProjectsAsync(),
});
