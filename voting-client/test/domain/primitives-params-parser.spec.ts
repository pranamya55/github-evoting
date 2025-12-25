/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {parsePrimitivesParams} from "../../src/domain/primitives-params-parser";

import authenticateVoterResponseJson from "../tools/data/authenticate-voter-response.json";

describe("Primitives params parser", function (): void {

	test("should parse all the primitives", function (): void {
		expect(() => parsePrimitivesParams(
			authenticateVoterResponseJson.votingClientPublicKeys,
			authenticateVoterResponseJson.primesMappingTable
		)).not.toThrow();
	});
});
