/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {VoterAnswers} from "e-voting-libraries-ui-kit";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {checkArgument} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {LATIN_ALPHABET} from "../latin-alphabet";

/**
 * Gets and validates the selected write-ins from the voter's answers.
 *
 * @param {VoterAnswers} voterAnswers - the voter's answers.
 *
 * @returns {ImmutableArray<string>} - the list of selected write-ins.
 */
export function getSelectedWriteIns(voterAnswers: VoterAnswers): ImmutableArray<string> {
	const selectedWriteIns: ImmutableArray<string> = ImmutableArray.from(voterAnswers.electionAnswers
		.flatMap(electionAnswers => electionAnswers.electionsInformation)
		.flatMap(electionInformationAnswers => electionInformationAnswers.chosenWriteIns ?? [])
		.map(chosenWriteIn => chosenWriteIn.writeIn)
		.filter(writeIn => writeIn !== null));

	checkArgument(selectedWriteIns.every(s_i_hat => !s_i_hat.includes(LATIN_ALPHABET.get(0))),
		`All selected write-in options must not contain the rank 0 character. [rank 0 character: ${LATIN_ALPHABET.get(0)}]`);
	checkArgument(selectedWriteIns.every(s_i_hat => s_i_hat.split('').every(char => LATIN_ALPHABET.includes(char))),
		"All characters in each selected write-in option must be in A_latin alphabet.");

	return selectedWriteIns;
}
