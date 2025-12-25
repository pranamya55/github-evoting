/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ExponentiationProofGroupVectorDeserializer;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.GqGroupVectorDeserializer;

public record PartiallyDecryptedEncryptedPCC(ContextIds contextIds,
											 int nodeId,
											 @JsonDeserialize(using = GqGroupVectorDeserializer.class)
											 GroupVector<GqElement, GqGroup> exponentiatedGammas,
											 @JsonDeserialize(using = ExponentiationProofGroupVectorDeserializer.class)
											 GroupVector<ExponentiationProof, ZqGroup> exponentiationProofs) implements HashableList {

	public PartiallyDecryptedEncryptedPCC {
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		checkNotNull(contextIds);
		checkNotNull(exponentiatedGammas);
		checkNotNull(exponentiationProofs);
		checkArgument(exponentiatedGammas.getGroup().hasSameOrderAs(exponentiationProofs.getGroup()),
				"The groups of the exponentiation gammas and the exponentation proofs must be of same order.");
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				contextIds,
				HashableBigInteger.from(BigInteger.valueOf(nodeId)),
				exponentiatedGammas,
				exponentiationProofs);
	}

}
