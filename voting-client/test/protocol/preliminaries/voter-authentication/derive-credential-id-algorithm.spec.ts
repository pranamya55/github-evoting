/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {deriveCredentialId} from "../../../../src/protocol/preliminaries/voter-authentication/derive-credential-id.algorithm";

describe("Derive credentialId algorithm", function (): void {

	const electionEventId: string = "34caee78ed3d4cf981ca06b659f558eb";
	const startVotingKey: string = "4d65ej2adb4ia6ghhzb52kg6";
	const credentialId: string = "9660D63A4AB22ECCEF143D213BAF3EF2";

	test("should return expected credentialId", async function (): Promise<void> {
		const derivedCredentialID: string = await deriveCredentialId(
			{
				electionEventId: electionEventId
			},
			startVotingKey
		);
		expect(derivedCredentialID).toBe(credentialId);
	});

});


