/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export enum ErrorStatus {
	// return by the OV Api
	BallotBoxEnded = 'BALLOT_BOX_ENDED',
	BallotBoxNotStarted = 'BALLOT_BOX_NOT_STARTED',
	StartVotingKeyInvalid = 'START_VOTING_KEY_INVALID',
	ExtendedFactorInvalid = 'EXTENDED_FACTOR_INVALID',
	AuthenticationAttemptsExceeded = 'AUTHENTICATION_ATTEMPTS_EXCEEDED',
	VotingCardBlocked = 'VOTING_CARD_BLOCKED',
	ConfirmationKeyIncorrect = 'CONFIRMATION_KEY_INCORRECT',
	ConfirmationKeyInvalid = 'CONFIRMATION_KEY_INVALID',
	ConfirmationAttemptsExceeded = 'CONFIRMATION_ATTEMPTS_EXCEEDED',
	VoteInvalid = 'VOTE_INVALID',
	TimestampMisaligned = 'TIMESTAMP_MISALIGNMENT',
	AuthenticationChallengeError = 'AUTHENTICATION_CHALLENGE_ERROR',
	VotingClientTimeError = 'VOTING_CLIENT_TIME_ERROR',

	// only in the voter portal
	ConnectionError = 'CONNECTION_ERROR',
	Default = 'ERROR',
}
