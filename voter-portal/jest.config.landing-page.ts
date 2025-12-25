/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {getJestProjectsAsync} from '@nx/jest';

export default async () => {
	const projects = await getJestProjectsAsync();

	// Filter out the voter-portal application and its libraries
	const filteredProjects = projects.filter((project) => {
		return !project.match(/^<rootDir>\\(apps|libs)\\voter-portal/);
	});

	return {
		projects: filteredProjects,
	};
};