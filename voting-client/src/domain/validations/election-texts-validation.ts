/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {
	Candidate,
	CandidatePosition,
	Election,
	ElectionInformation,
	ElectionTexts,
	EmptyList,
	EmptyPosition,
	List,
	ReferencedElectionInformation,
	WriteInPosition
} from "e-voting-libraries-ui-kit";
import {validateTranslatableText, validateXsToken} from "./validations";

/**
 * Validates the input election texts.
 *
 * @param {ElectionTexts} toValidate - the election texts to validate.
 *
 * @returns {ElectionTexts} - the validated election texts.
 */
export function validateElectionTexts(toValidate: ElectionTexts): ElectionTexts {
	checkNotNull(toValidate);
	validateXsToken(toValidate.electionGroupIdentification);
	validateXsToken(toValidate.domainOfInfluence);
	checkArgument(toValidate.electionGroupPosition >= 0, "The election group position must be positive.");
	checkNotNull(toValidate.electionsInformation).forEach(electionInformation => validateElectionInformation(electionInformation));
	checkArgument(toValidate.electionsInformation.length > 0, "There must be at least one election information.");
	return toValidate;
}

function validateElectionInformation(electionInformation: ElectionInformation): ElectionInformation {
	checkNotNull(electionInformation);
	validateElection(electionInformation.election);
	checkNotNull(electionInformation.candidates).forEach(candidate => validateCandidate(candidate));
	checkNotNull(electionInformation.lists).forEach(list => validateList(list));
	const numberOfMandates: number = electionInformation.election.numberOfMandates;
	validateEmptyList(electionInformation.emptyList, numberOfMandates);
	checkNotNull(electionInformation.writeInPositions).forEach(writeInPosition => validateWriteInPosition(writeInPosition));
	if (electionInformation.writeInPositions.length > 0) {
		checkArgument(electionInformation.election.writeInsAllowed, "The election must have write-in positions since it allows write-ins.");
		checkArgument(electionInformation.writeInPositions.length === numberOfMandates,
			"The size of the write-in positions list must be equal to the number of mandates.");
	}
	return electionInformation;
}

function validateElection(election: Election): Election {
	checkNotNull(election);
	validateXsToken(election.electionIdentification);
	validateTranslatableText(election.electionDescription);
	if (election.electionRulesExplanation) {
		validateTranslatableText(election.electionRulesExplanation);
	}
	checkArgument(election.electionPosition >= 0, "The election position must be positive.");
	checkArgument(election.numberOfMandates > 0, "The number of mandates must be strictly positive.");
	checkArgument(election.candidateAccumulation >= 0, "The candidate accumulation must be positive.");
	checkArgument(election.minimalCandidateSelectionInList >= 0, "The minimal candidate selection in list must be positive.");
	if (election.referencedElectionsInformation) {
		checkNotNull(election.referencedElectionsInformation).forEach(referencedElectionInformation => validateReferencedElectionInformation(referencedElectionInformation));
	}
	return election;
}

function validateCandidate(candidate: Candidate): Candidate {
	checkNotNull(candidate);
	validateXsToken(candidate.candidateIdentification);
	checkArgument(candidate.position === undefined || candidate.position >= 0, "The position must be positive.");
	checkArgument(candidate.eligibility !== undefined, "The eligibility must be valid.");
	validateTranslatableText(candidate.displayCandidateLine1);
	validateTranslatableText(candidate.displayCandidateLine2);
	if (candidate.displayCandidateLine3) {
		validateTranslatableText(candidate.displayCandidateLine3);
	}
	if (candidate.displayCandidateLine4) {
		validateTranslatableText(candidate.displayCandidateLine4);
	}
	if (candidate.displayCandidateLine5) {
		validateTranslatableText(candidate.displayCandidateLine5);
	}
	return candidate;
}

function validateList(list: List): List {
	checkNotNull(list);
	validateXsToken(list.listIdentification);
	validateTranslatableText(list.listDescription);
	checkArgument(list.listOrderOfPrecedence >= 0, "The list order of precedence must be positive.");
	checkNotNull(list.candidatePositions).forEach(candidatePosition => validateCandidatePosition(candidatePosition));
	checkArgument(list.candidatePositions.length > 0, "There must be at least one candidate position.");
	validateTranslatableText(list.displayListLine1);
	if (list.displayListLine2) {
		validateTranslatableText(list.displayListLine2);
	}
	if (list.displayListLine3) {
		validateTranslatableText(list.displayListLine3);
	}
	if (list.displayListLine4) {
		validateTranslatableText(list.displayListLine4);
	}
	if (list.displayListLine5) {
		validateTranslatableText(list.displayListLine5);
	}
	return list;
}

function validateEmptyList(emptyList: EmptyList, numberOfMandates: number): EmptyList {
	checkNotNull(emptyList);
	validateXsToken(emptyList.listIdentification);
	validateTranslatableText(emptyList.listDescription);
	checkNotNull(emptyList.emptyPositions).forEach(emptyPosition => validateEmptyPosition(emptyPosition));
	checkArgument(emptyList.emptyPositions.length === numberOfMandates, "There must be as many empty positions as number of mandates.");
	return emptyList;
}

function validateWriteInPosition(writeInPosition: WriteInPosition): WriteInPosition {
	checkNotNull(writeInPosition);
	validateXsToken(writeInPosition.writeInPositionIdentification);
	checkArgument(writeInPosition.position >= 0, "The position must be positive.");
	validateTranslatableText(writeInPosition.returnCodeWriteInDescription);
	return writeInPosition;
}

function validateReferencedElectionInformation(referenceElectionInformation: ReferencedElectionInformation): ReferencedElectionInformation {
	checkNotNull(referenceElectionInformation);
	validateXsToken(referenceElectionInformation.referencedElection);
	checkArgument(referenceElectionInformation.electionRelation >= 0, "The election relation must be positive.");
	return referenceElectionInformation;
}

function validateCandidatePosition(candidatePosition: CandidatePosition): CandidatePosition {
	checkNotNull(candidatePosition);
	validateXsToken(candidatePosition.candidateListIdentification);
	checkArgument(candidatePosition.positionOnList >= 0, "The position on list must be positive.");
	validateXsToken(candidatePosition.candidateIdentification);
	return candidatePosition;
}

function validateEmptyPosition(emptyPosition: EmptyPosition): EmptyPosition {
	checkNotNull(emptyPosition);
	validateXsToken(emptyPosition.emptyPositionIdentification);
	checkArgument(emptyPosition.positionOnList >= 0, "The position on list must be positive.");
	validateTranslatableText(emptyPosition.emptyPositionText);
	return emptyPosition;
}
