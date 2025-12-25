/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.utils.ByteArrays.cutToBitLength;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.stringToByteArray;
import static ch.post.it.evoting.evotinglibraries.domain.validations.StartVotingKeyValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base16;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Implements the DeriveCredentialId algorithm.
 */
@Service
public class DeriveCredentialIdAlgorithm {

	private final Hash hash;
	private final Base16 base16;
	private final Argon2 argon2;

	public DeriveCredentialIdAlgorithm(final Hash hash,
			final Base16 base16,
			@Qualifier("argon2LessMemory")
			final Argon2 argon2) {
		this.hash = hash;
		this.base16 = base16;
		this.argon2 = argon2;
	}

	/**
	 * Derives a voter's identifier credentialID<sub>id</sub> from the Start Voting Key SVK<sub>id</sub>.
	 *
	 * @param electionEventId ee, the election event id. Must be non-null and a valid UUID.
	 * @param startVotingKey  SVK<sub>id</sub>, the Start Voting Key. Must be non-null and a valid Base32 string of size l<sub>SVK</sub>.
	 * @return the derived credentialID<sub>id</sub>.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws IllegalArgumentException  if the Start Voting Key's size is not
	 *                                   l<sub>SVK</sub>={@value ch.post.it.evoting.evotinglibraries.domain.common.Constants#SVK_LENGTH}.
	 * @throws FailedValidationException if the election event id is not a valid UUID or the start voting key is not a valid Base32 string.
	 */
	@SuppressWarnings("java:S117")
	public String deriveCredentialId(final String electionEventId, final String startVotingKey) {

		// Context.
		final String ee = validateUUID(electionEventId);

		// Input.
		final String SVK_id = validate(startVotingKey);

		// Operation.
		final ImmutableByteArray recursiveHash = hash.recursiveHash(HashableList.of(HashableString.from(ee), HashableString.from("credentialId")));
		final ImmutableByteArray salt = cutToBitLength(recursiveHash, 128);

		final ImmutableByteArray bCredentialID_id = argon2.getArgon2id(stringToByteArray(SVK_id), salt);

		return base16.base16Encode(cutToBitLength(bCredentialID_id, 128));
	}

}
