/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static com.google.common.base.Preconditions.checkArgument;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;

/**
 * Payload containing the exponentiated gammas and corresponding exponentiation proofs of all control components.
 *
 * @param controlComponentPartialDecryptPayloads the list of control component partial decrypt payloads.
 */
public record CombinedControlComponentPartialDecryptPayload(
		ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads) implements HashableList {

	public CombinedControlComponentPartialDecryptPayload {
		validate(controlComponentPartialDecryptPayloads);
		checkArgument(
				allEqual(controlComponentPartialDecryptPayloads.stream(), payload -> payload.getPartiallyDecryptedEncryptedPCC().contextIds()),
				"All control component partial decrypt payloads must have the same contextIds.");
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(HashableList.from(controlComponentPartialDecryptPayloads));
	}
}
