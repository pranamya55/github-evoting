/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

/* eslint-disable */
export default {
	displayName: 'voter-portal-util-types',
	preset: '../../../jest.preset.js',
	testEnvironment: 'node',
	transform: {
		'^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }],
	},
	moduleFileExtensions: ['ts', 'js', 'html'],
	coverageDirectory: '../../../coverage/libs/voter-portal/util-types',
};
