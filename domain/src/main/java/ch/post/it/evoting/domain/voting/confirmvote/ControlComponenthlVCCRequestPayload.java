/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;

import java.util.Comparator;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;

public record ControlComponenthlVCCRequestPayload(ImmutableList<ControlComponenthlVCCSharePayload> controlComponenthlVCCPayloads)
		implements HashableList {

	public ControlComponenthlVCCRequestPayload {
		validate(controlComponenthlVCCPayloads);
	}

	public ImmutableList<ControlComponenthlVCCSharePayload> controlComponenthlVCCPayloads() {
		return controlComponenthlVCCPayloads.stream()
				.sorted(Comparator.comparingInt(ControlComponenthlVCCSharePayload::getNodeId))
				.collect(toImmutableList());
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(HashableList.from(controlComponenthlVCCPayloads));
	}
}
