/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export default {
  preset: "ts-jest/presets/js-with-ts",
  testEnvironment: "node",
  transformIgnorePatterns: ["<rootDir>/node_modules/"],
  moduleNameMapper: {
    "crypto-primitives-ts/(.*)": "<rootDir>/vendor/crypto-primitives-ts/$1"
  },
  coveragePathIgnorePatterns: ["<rootDir>/vendor"]
};