/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray.concat;
import static ch.post.it.evoting.cryptoprimitives.utils.ByteArrays.cutToBitLength;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.stringToByteArray;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ExtendedAuthenticationFactorValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.StartVotingKeyValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
@ConditionalOnProperty("role.isSetup")
public class DeriveBaseAuthenticationChallengeAlgorithm {

	private final Hash hash;
	private final Argon2 argon2;
	private final Base64 base64;

	public DeriveBaseAuthenticationChallengeAlgorithm(final Hash hash,
			@Qualifier("argon2LessMemory")
			final Argon2 argon2,
			final Base64 base64) {

		this.hash = hash;
		this.argon2 = argon2;
		this.base64 = base64;
	}

	/**
	 * Derives the base authentication challenge for the given election event from the given start voting key and extended authentication factor
	 *
	 * @param electionEventId                    ee, the identifier of the election event. Must be a valid UUID.
	 * @param extendedAuthenticationFactorLength l<sub>EA</sub>, character length of the extended authentication factor. Must be part of the possible
	 *                                           extended authentication factor character lengths.
	 * @param startVotingKey                     SVK<sub>id</sub>, a start voting key. Must be a valid Base32 string without padding of length
	 *                                           l<sub>SVK</sub>.
	 * @param extendedAuthenticationFactor       EA<sub>id</sub>, an extended authentication factor. Must be a valid Base10 string of length
	 *                                           l<sub>EA</sub>.
	 * @return the base authentication challenge as a string.
	 * @throws NullPointerException      if any of the inputs is null.
	 * @throws FailedValidationException if
	 *                                   <ul>
	 *                                       <li>the election event id is not a valid UUID</li>
	 *                                       <li>the start voting key is not a valid Base32 string</li>
	 *                                   </ul>
	 * @throws IllegalArgumentException  if
	 *                                   <ul>
	 *                                       <li>the start voting key is not of size l<sub>SVK</sub></li>
	 *                                       <li>the extended authentication factor is not a valid Base10 string of length l<sub>EA</sub></li>
	 *                                   </ul>
	 */
	@SuppressWarnings("java:S117")
	public String deriveBaseAuthenticationChallenge(final String electionEventId, final int extendedAuthenticationFactorLength,
			final String startVotingKey, final String extendedAuthenticationFactor) {

		// Context.
		final String ee = validateUUID(electionEventId);
		final int l_EA = extendedAuthenticationFactorLength;

		// Input.
		final String SVK_id = validate(startVotingKey);
		final String EA_id = validate(extendedAuthenticationFactor, l_EA);

		// Operation.
		final ImmutableByteArray salt_auth = cutToBitLength(hash.recursiveHash(HashableString.from(ee), HashableString.from("hAuth")), 128);
		final ImmutableByteArray k = concat(stringToByteArray(EA_id), stringToByteArray("Auth"), stringToByteArray(SVK_id));
		final ImmutableByteArray bhAuth_id = argon2.getArgon2id(k, salt_auth);

		return base64.base64Encode(bhAuth_id);
	}
}
