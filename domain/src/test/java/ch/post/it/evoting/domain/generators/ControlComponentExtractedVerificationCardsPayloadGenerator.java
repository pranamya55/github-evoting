/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.RandomFactory.createRandom;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;

import java.util.function.Function;
import java.util.stream.IntStream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCardSet;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedVerificationCardGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class ControlComponentExtractedVerificationCardsPayloadGenerator {

	private static final Hash hash = createHash();
	private static final Random random = createRandom();
	private static final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	final GqGroup encryptionGroup;
	final ExtractedVerificationCardGenerator extractedVerificationCardGenerator;

	public ControlComponentExtractedVerificationCardsPayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
		this.extractedVerificationCardGenerator = new ExtractedVerificationCardGenerator(encryptionGroup);
	}

	public ControlComponentExtractedVerificationCardsPayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public ImmutableList<ControlComponentExtractedVerificationCardsPayload> generate() {
		final String electionEventId = uuidGenerator.generate();

		final int numberOfVerificationCardSets = random.genRandomInteger(4) + 1;
		final ImmutableList<String> verificationCardSetIds = IntStream.range(0, numberOfVerificationCardSets)
				.mapToObj(i -> uuidGenerator.generate())
				.collect(toImmutableList());

		return generate(electionEventId, verificationCardSetIds);
	}

	public ImmutableList<ControlComponentExtractedVerificationCardsPayload> generate(final ControlComponentExtractedElectionEventPayload controlComponentExtractedElectionEventPayload) {
		final ExtractedElectionEvent extractedElectionEvent = controlComponentExtractedElectionEventPayload.getExtractedElectionEvent();
		return generate(extractedElectionEvent.electionEventId(),
				extractedElectionEvent.extractedVerificationCardSets().stream()
						.map(ExtractedVerificationCardSet::verificationCardSetId)
						.collect(toImmutableList()));
	}

	public ImmutableList<ControlComponentExtractedVerificationCardsPayload> generate(final String electionEventId,
			final ImmutableList<String> verificationCardSetIds) {

		final int numberOfVerificationCards = random.genRandomInteger(2) + 2; // at least two verification cards for testing purposes.
		final ImmutableList<ExtractedVerificationCard> baseExtractedVerificationCards = IntStream.range(0,
						verificationCardSetIds.size() * numberOfVerificationCards)
				.mapToObj(i -> {
					final ExtractedVerificationCard extractedVerificationCard = extractedVerificationCardGenerator.generate();
					return new ExtractedVerificationCard(
							extractedVerificationCard.verificationCardId(),
							verificationCardSetIds.get(i % verificationCardSetIds.size()),
							extractedVerificationCard.encryptedVote(),
							extractedVerificationCard.hashedLongVoteCastReturnCodeShares());
				})
				.collect(toImmutableList());

		final ImmutableMap<String, ImmutableList<String>> hashedLongVoteCastReturnCodeShares = verificationCardSetIds.stream()
				.collect(toImmutableMap(
						Function.identity(),
						verificationCardSetId -> random.genRandomInteger(2) == 0 ?
								ControlComponentNode.ids().stream()
										.map(j -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
										.collect(toImmutableList()) :
								ImmutableList.emptyList())
				);

		return ControlComponentNode.ids().stream()
				.map(nodeId -> generate(electionEventId, nodeId, baseExtractedVerificationCards, hashedLongVoteCastReturnCodeShares))
				.collect(toImmutableList());
	}

	public ControlComponentExtractedVerificationCardsPayload generate(final String electionEventId, final int nodeId,
			final ImmutableList<ExtractedVerificationCard> baseExtractedVerificationCards,
			final ImmutableMap<String, ImmutableList<String>> hashedLongVoteCastReturnCodeShares) {
		final ImmutableList<ExtractedVerificationCard> extractedVerificationCards = baseExtractedVerificationCards.stream()
				.map(baseExtractedVerificationCard -> new ExtractedVerificationCard(
						baseExtractedVerificationCard.verificationCardId(),
						baseExtractedVerificationCard.verificationCardSetId(),
						baseExtractedVerificationCard.encryptedVote(),
						hashedLongVoteCastReturnCodeShares.get(baseExtractedVerificationCard.verificationCardSetId()))
				).collect(toImmutableList());

		final ControlComponentExtractedVerificationCardsPayload controlComponentExtractedVerificationCardsPayload = new ControlComponentExtractedVerificationCardsPayload(
				encryptionGroup, electionEventId, nodeId, extractedVerificationCards);

		final ImmutableByteArray payloadHash = hash.recursiveHash(controlComponentExtractedVerificationCardsPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		controlComponentExtractedVerificationCardsPayload.setSignature(signature);

		return controlComponentExtractedVerificationCardsPayload;
	}
}
