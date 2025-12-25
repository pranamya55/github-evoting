/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.idempotence;

import java.util.function.Supplier;

public enum IdempotenceContext implements Supplier<String> {
	SAVE_RETURN_CODES_MAPPING_TABLE,
	SAVE_VOTER_AUTHENTICATION_DATA,
	SAVE_SETUP_COMPONENT_PUBLIC_KEYS,
	SAVE_SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORES,
	SAVE_ELECTION_EVENT_CONTEXT,
	COMPUTE_CHUNK,
	CONFIRM_VOTE;

	@Override
	public String get() {
		return this.name();
	}
}
