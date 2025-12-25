/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

public final class Constants {

	// Voting Phase
	public static final int MAX_AUTHENTICATION_ATTEMPTS = 5;
	public static final int MAX_CONFIRMATION_ATTEMPTS = 5;

	// Dispute Resolver
	public static final String CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT = "controlComponentExtractedElectionEventPayload.%s.json";
	public static final String CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT = "controlComponentExtractedVerificationCardsPayload.%s.json";
	public static final String DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME = "disputeResolverResolvedConfirmedVotesPayload.json";

	private Constants() {
		// Avoid instantiation.
	}
}