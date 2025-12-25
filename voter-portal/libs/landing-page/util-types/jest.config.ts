/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

/* eslint-disable */
export default {
	displayName: 'landing-page-util-types',
	preset: '../../../jest.preset.js',
	testEnvironment: 'node',
	transform: {
		'^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }],
	},
	moduleFileExtensions: ['ts', 'js', 'html'],
	coverageDirectory: '../../../coverage/libs/landing-page/util-types',
};
