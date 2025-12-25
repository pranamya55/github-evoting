/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import java.math.BigInteger;

/**
 * Class for holding constants used in the rest web services.
 */
public final class Constants {

	// We define a dummy election event ID to test the proper deployment of the voter portal without setting up an actual election event.
	public static final String DUMMY_ELECTION_EVENT_ID = "00000000000000000000000000000000";

	public static final String PARAMETER_VALUE_ELECTION_EVENT_ID = "electionEventId";

	public static final String PARAMETER_VALUE_VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	public static final String PARAMETER_VALUE_VERIFICATION_CARD_ID = "verificationCardId";

	public static final String PARAMETER_VALUE_CREDENTIAL_ID = "credentialId";

	public static final String VOTING_CARD_ID = "votingCardId";

	public static final BigInteger TWO_POW_256 = BigInteger.ONE.shiftLeft(256);

	// Avoid instantiation.
	private Constants() {
	}
}
