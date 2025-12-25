/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AuthenticateVoterResponsePayload,
	VerificationCardKeystore,
	VerificationCardState,
	VoterAuthenticationData,
	VoterMaterial,
	VotingClientPublicKeys
} from "../authenticate-voter.types";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {validateBase64String, validateUUID} from "./validations";
import {validateShortChoiceReturnCodes} from "./short-choice-return-code-validation";
import {validateShortVoteCastReturnCodes} from "./short-vote-cast-return-code-validation";

/**
 * Validates the authenticate voter response payload.
 *
 * @param {AuthenticateVoterResponsePayload} toValidate - the authenticate voter response payload to validate.
 */
export function validateAuthenticateVoterResponsePayload(toValidate: AuthenticateVoterResponsePayload): void {
	checkNotNull(toValidate);
	checkNotNull(toValidate.verificationCardState);
	validateVoterMaterial(toValidate.voterMaterial, toValidate.verificationCardState);

	if (toValidate.verificationCardState === VerificationCardState.INITIAL || toValidate.verificationCardState === VerificationCardState.SENT) {
		validateVoterAuthenticationData(toValidate.voterAuthenticationData);
		validateVerificationCardKeystore(toValidate.verificationCardKeystore);
		validateVotingClientPublicKeys(toValidate.votingClientPublicKeys);
		checkNotNull(toValidate.primesMappingTable);
		// In order to further validate the primes mapping table, the primitives parameters must be parsed. This is done in primitives-params-parser.ts.
	}
}

/**
 * Validates the voter material.
 * <p>
 * The presence of fields depends on if it is (and when) a re-login.
 * <p>
 * <ul>
 *     <li>first authentication: only {@code votesTexts} and/or {@code electionsTexts} are present.</li>
 *     <li>authentication after vote has been sent, before confirmed: {@code votesTexts}, {@code electionsTexts} and {@code shortChoiceReturnCodes} are present.</li>
 *     <li>authentication after vote has been confirmed: only {@code shortVoteCastReturnCode} is present.</li>
 * </ul>
 *
 * @param voterMaterial the voter material to validate.
 * @param verificationCardState the verification card state.
 */
function validateVoterMaterial(voterMaterial: VoterMaterial, verificationCardState: VerificationCardState): void {
	checkNotNull(voterMaterial);
	switch (verificationCardState) {
		case VerificationCardState.INITIAL:
			checkArgument(voterMaterial.votesTexts.length !== 0 || voterMaterial.electionsTexts.length !== 0,
				"There must be at least one vote texts or election texts.");
			checkArgument(voterMaterial.shortChoiceReturnCodes === undefined);
			checkArgument(voterMaterial.shortVoteCastReturnCode === undefined);
			break;
		case VerificationCardState.SENT:
			checkArgument(voterMaterial.votesTexts.length !== 0 || voterMaterial.electionsTexts.length !== 0,
				"There must be at least one vote texts or election texts.");
			validateShortChoiceReturnCodes(voterMaterial.shortChoiceReturnCodes);
			checkArgument(voterMaterial.shortVoteCastReturnCode === undefined);
			break;
		case VerificationCardState.CONFIRMED:
			checkArgument(voterMaterial.votesTexts.length === 0, "There must be no vote texts.");
			checkArgument(voterMaterial.electionsTexts.length === 0, "There must be no election texts.");
			checkArgument(voterMaterial.shortChoiceReturnCodes === undefined);
			validateShortVoteCastReturnCodes(voterMaterial.shortVoteCastReturnCode);
			break;
		default:
	}
}

function validateVoterAuthenticationData(voterAuthenticationData: VoterAuthenticationData): void {
	checkNotNull(voterAuthenticationData);
	validateUUID(voterAuthenticationData.electionEventId);
	validateUUID(voterAuthenticationData.verificationCardSetId);
	validateUUID(voterAuthenticationData.ballotBoxId);
	validateUUID(voterAuthenticationData.verificationCardId);
	validateUUID(voterAuthenticationData.votingCardId);
	validateUUID(voterAuthenticationData.credentialId);
}

function validateVerificationCardKeystore(verificationCardKeystore: VerificationCardKeystore): void {
	checkNotNull(verificationCardKeystore);
	validateUUID(verificationCardKeystore.verificationCardId);
	validateBase64String(verificationCardKeystore.verificationCardKeystore);
}

function validateVotingClientPublicKeys(votingClientPublicKeys: VotingClientPublicKeys): void {
	checkNotNull(votingClientPublicKeys);
	checkNotNull(votingClientPublicKeys.encryptionParameters);
	checkNotNull(votingClientPublicKeys.electionPublicKey).forEach(element => checkNotNull(element));
	checkArgument(votingClientPublicKeys.electionPublicKey.length > 0, "The election public key must not be empty.");
	checkNotNull(votingClientPublicKeys.choiceReturnCodesEncryptionPublicKey).forEach(element => checkNotNull(element));
	checkArgument(votingClientPublicKeys.choiceReturnCodesEncryptionPublicKey.length > 0, "The choice return codes encryption public key must not be empty.");
}