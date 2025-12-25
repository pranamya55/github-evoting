/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public record VotingCardSearchDto(ImmutableList<VotingCardDto> votingCards,
								  @JsonProperty("_metadata")
								  Metadata metadata) {

	public VotingCardSearchDto {
		checkNotNull(votingCards);
		checkNotNull(metadata);
	}

	public record Metadata(long limit, long totalCount) {

		public Metadata {
			checkArgument(limit >= 0, "Limit must be greater than or equal to 0");
			checkArgument(totalCount >= 0, "Total count must be greater than or equal to 0");
		}
	}
}
