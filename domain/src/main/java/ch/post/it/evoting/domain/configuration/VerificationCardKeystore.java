/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;

public record VerificationCardKeystore(String verificationCardId,
									   String verificationCardKeystore) implements HashableList {
	private static final int keystoreSize = 572;

	public VerificationCardKeystore {
		validateUUID(verificationCardId);
		validateBase64Encoded(verificationCardKeystore);
		validateKeystoreSize(verificationCardKeystore);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(verificationCardId),
				HashableString.from(verificationCardKeystore));
	}

	public static void validateKeystoreSize(final String verificationCardKeystore) {
		checkArgument(verificationCardKeystore.length() == keystoreSize && verificationCardKeystore.indexOf("=") == keystoreSize - 1,
				"The verification card keystore must have a size of %s with only the last character being the equals sign (=).", keystoreSize);
	}
}
