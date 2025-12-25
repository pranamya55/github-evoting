/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.GqGroupVectorDeserializer;

public record LongChoiceReturnCodeShare(String electionEventId,
										String verificationCardSetId,
										String verificationCardId,
										int nodeId,
										@JsonDeserialize(using = GqGroupVectorDeserializer.class)
										GroupVector<GqElement, GqGroup> longChoiceReturnCodeShare) implements HashableList {

	public LongChoiceReturnCodeShare {
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		validateUUID(verificationCardId);
		checkNotNull(longChoiceReturnCodeShare);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(electionEventId),
				HashableString.from(verificationCardSetId),
				HashableString.from(verificationCardId),
				HashableBigInteger.from(BigInteger.valueOf(nodeId)),
				longChoiceReturnCodeShare);
	}

}
