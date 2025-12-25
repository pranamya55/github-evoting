/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generateenclongcodeshares;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

public class EncryptedLongReturnCodeSharesEntityKey {

	private int chunkId;
	private String verificationCardSetId;

	public EncryptedLongReturnCodeSharesEntityKey() {
	}

	public EncryptedLongReturnCodeSharesEntityKey(final int chunkId, final String verificationCardSetId) {
		this.chunkId = chunkId;
		checkArgument(chunkId >= 0, "The chunkId must be positive.");
		this.verificationCardSetId = validateUUID(verificationCardSetId);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final EncryptedLongReturnCodeSharesEntityKey that = (EncryptedLongReturnCodeSharesEntityKey) o;
		return chunkId == that.chunkId && Objects.equals(verificationCardSetId, that.verificationCardSetId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(chunkId, verificationCardSetId);
	}
}
