/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {SendVoteResponsePayload} from "../send-vote.types";
import {validateShortChoiceReturnCodes} from "./short-choice-return-code-validation";

/**
 * Validates the send vote response payload.
 *
 * @param {SendVoteResponsePayload} toValidate - the send vote response payload to validate.
 */
export function validateSendVoteResponsePayload(toValidate: SendVoteResponsePayload): void {
	checkNotNull(toValidate);
	validateShortChoiceReturnCodes(toValidate.shortChoiceReturnCodes);
}