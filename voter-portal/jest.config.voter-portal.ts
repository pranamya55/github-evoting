/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {getJestProjectsAsync} from '@nx/jest';

export default async () => {
	const projects = await getJestProjectsAsync();

	// Filter out the lading page application and its libraries
	const filteredProjects = projects.filter((project) => {
		return !project.match(/^<rootDir>\\(apps|libs)\\landing-page/);
	});

	return {
		projects: filteredProjects,
	};
};