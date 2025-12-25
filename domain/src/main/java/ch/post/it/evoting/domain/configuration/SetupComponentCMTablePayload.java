/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.Entry.comparingByKey;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_CAST_RETURN_CODE_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;

@JsonDeserialize(builder = SetupComponentCMTablePayload.Builder.class)
@JsonPropertyOrder({ "electionEventId", "verificationCardSetId", "chunkId", "returnCodesMappingTable", "signature" })
public final class SetupComponentCMTablePayload implements SignedPayload {

	private static final int ENCODED_CHOICE_RETURN_CODE_SIZE = BASE64_ENCODED_HASH_OUTPUT_LENGTH;
	private static final int ENCODED_CAST_RETURN_CODE_SIZE = BASE64_ENCODED_CAST_RETURN_CODE_LENGTH;
	private final String electionEventId;
	private final String verificationCardSetId;
	private final int chunkId;
	private final ImmutableMap<String, String> returnCodesMappingTable;
	private CryptoPrimitivesSignature signature;

	private SetupComponentCMTablePayload(final String electionEventId, final String verificationCardSetId, final int chunkId,
			final ImmutableMap<String, String> returnCodesMappingTable, final CryptoPrimitivesSignature signature) {
		this.electionEventId = validateUUID(electionEventId);
		this.verificationCardSetId = validateUUID(verificationCardSetId);
		this.chunkId = chunkId;
		this.returnCodesMappingTable = checkNotNull(returnCodesMappingTable);
		this.signature = signature; // signature may be null

		checkArgument(chunkId >= 0, "The chunkId must be positive.");

		checkArgument(this.returnCodesMappingTable.keySet().stream().parallel()
						.allMatch(key -> validateBase64Encoded(key).length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH),
				String.format("The CM table's keys must be valid Base64 string of length %s.", BASE64_ENCODED_HASH_OUTPUT_LENGTH));

		checkArgument(this.returnCodesMappingTable.values().stream().parallel()
						.allMatch(value -> validateBase64Encoded(value).length() == ENCODED_CHOICE_RETURN_CODE_SIZE
								|| value.length() == ENCODED_CAST_RETURN_CODE_SIZE),
				String.format("The CM table's values must be valid Base64 string of length %s or %s.", ENCODED_CHOICE_RETURN_CODE_SIZE,
						ENCODED_CAST_RETURN_CODE_SIZE));
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public int getChunkId() {
		return chunkId;
	}

	public ImmutableMap<String, String> getReturnCodesMappingTable() {
		return returnCodesMappingTable;
	}

	public CryptoPrimitivesSignature getSignature() {
		return signature;
	}

	@Override
	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = signature;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final SetupComponentCMTablePayload that = (SetupComponentCMTablePayload) o;

		return electionEventId.equals(that.electionEventId) &&
				verificationCardSetId.equals(that.verificationCardSetId) &&
				Objects.equals(chunkId, that.chunkId) &&
				returnCodesMappingTable.equals(that.returnCodesMappingTable) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, verificationCardSetId, chunkId, returnCodesMappingTable, signature);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {

		final ImmutableList<HashableList> hashableReturnCodesMappingTable = returnCodesMappingTable.entrySet().stream()
				.sorted(comparingByKey()) // Ensures the hashable form is deterministic.
				.map(entry -> HashableList.of(HashableString.from(entry.key()), HashableString.from(entry.value())))
				.collect(toImmutableList());

		return ImmutableList.of(
				HashableString.from(electionEventId),
				HashableString.from(verificationCardSetId),
				HashableBigInteger.from(BigInteger.valueOf(chunkId)),
				HashableList.from(hashableReturnCodesMappingTable));

	}

	@JsonPOJOBuilder(withPrefix = "set")
	public static class Builder {

		private String electionEventId;
		private String verificationCardSetId;
		private int chunkId;
		private ImmutableMap<String, String> returnCodesMappingTable;
		private CryptoPrimitivesSignature signature;

		@JsonProperty("electionEventId")
		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		@JsonProperty("verificationCardSetId")
		public Builder setVerificationCardSetId(final String verificationCardSetId) {
			this.verificationCardSetId = verificationCardSetId;
			return this;
		}

		@JsonProperty("chunkId")
		public Builder setChunkId(final int chunkId) {
			this.chunkId = chunkId;
			return this;
		}

		@JsonProperty("returnCodesMappingTable")
		public Builder setReturnCodesMappingTable(final ImmutableMap<String, String> returnCodesMappingTable) {
			this.returnCodesMappingTable = returnCodesMappingTable;
			return this;
		}

		@JsonProperty("signature")
		public Builder setSignature(final CryptoPrimitivesSignature signature) {
			this.signature = signature;
			return this;
		}

		public SetupComponentCMTablePayload build() {
			return new SetupComponentCMTablePayload(electionEventId, verificationCardSetId, chunkId, returnCodesMappingTable, signature);
		}
	}
}
