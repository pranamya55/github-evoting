/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.RandomFactory.createRandom;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedVerificationCardGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class DisputeResolverResolvedConfirmedVotesPayloadGenerator {

	private static final Hash hash = createHash();
	private static final Random random = createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	final ExtractedVerificationCardGenerator extractedVerificationCardGenerator = new ExtractedVerificationCardGenerator();

	public DisputeResolverResolvedConfirmedVotesPayload generate() {
		final String electionEventId = uuidGenerator.generate();

		return generate(electionEventId);
	}

	public DisputeResolverResolvedConfirmedVotesPayload generate(final String electionEventId) {

		final int numberOfConfirmedVotes = random.genRandomInteger(10) + 2; // at least two confirmed votes for testing purposes.
		final ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes = Stream.generate(extractedVerificationCardGenerator::generate)
				.limit(numberOfConfirmedVotes)
				.map(extractedVerificationCard -> new ResolvedConfirmedVote(extractedVerificationCard.verificationCardId(),
						extractedVerificationCard.verificationCardSetId(), extractedVerificationCard.hashedLongVoteCastReturnCodeShares()))
				.collect(toImmutableList());

		final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload = new DisputeResolverResolvedConfirmedVotesPayload(
				electionEventId, resolvedConfirmedVotes);

		final ImmutableByteArray payloadHash = hash.recursiveHash(disputeResolverResolvedConfirmedVotesPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		disputeResolverResolvedConfirmedVotesPayload.setSignature(signature);

		return disputeResolverResolvedConfirmedVotesPayload;
	}
}
