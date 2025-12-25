/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVectorElement;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;

/**
 * Represents an entry, say the i-th entry, of the choiceReturnCodesToEncodedVotingOptions table.
 *
 * @param choiceReturnCode    CC<sub>i</sub>, the choice return code. Must be non-null.
 * @param encodedVotingOption p&#771;<sub>i</sub>, the encoded voting option. Must be non-null.
 */
public record ChoiceReturnCodeToEncodedVotingOptionEntry(String choiceReturnCode,
														 PrimeGqElement encodedVotingOption) implements GroupVectorElement<GqGroup>, HashableList {

	public ChoiceReturnCodeToEncodedVotingOptionEntry {
		checkNotNull(choiceReturnCode);
		checkNotNull(encodedVotingOption);
	}

	@JsonIgnore
	@Override
	public GqGroup getGroup() {
		return encodedVotingOption.getGroup();
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(choiceReturnCode),
				HashableBigInteger.from(encodedVotingOption.getValue()));
	}
}
