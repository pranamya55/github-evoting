/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {ConfirmVoteResponsePayload} from "../confirm-vote.types";
import {validateShortVoteCastReturnCodes} from "./short-vote-cast-return-code-validation";

/**
 * Validates the confirm vote response payload.
 *
 * @param {ConfirmVoteResponsePayload} toValidate - the confirm vote response payload to validate.
 */
export function validateConfirmVoteResponsePayload(toValidate: ConfirmVoteResponsePayload): void {
	checkNotNull(toValidate);
	validateShortVoteCastReturnCodes(toValidate.shortVoteCastReturnCode);
}