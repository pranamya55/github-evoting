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
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class SetupComponentTallyDataPayloadGenerator {

	private static final Hash hash = createHash();
	private static final Random random = createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private final GqGroup encryptionGroup;
	private final ElGamalGenerator elGamalGenerator;

	public SetupComponentTallyDataPayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
		this.elGamalGenerator = new ElGamalGenerator(encryptionGroup);
	}

	public SetupComponentTallyDataPayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public SetupComponentTallyDataPayload generate() {
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final int numberOfEligibleVoters = random.genRandomInteger(10) + 1;

		return generate(electionEventId, verificationCardSetId, numberOfEligibleVoters);
	}

	public SetupComponentTallyDataPayload generate(final String electionEventId, final String verificationCardSetId,
			final int numberOfEligibleVoters) {
		final ImmutableList<String> verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(numberOfEligibleVoters)
				.collect(toImmutableList());
		return generate(electionEventId, verificationCardSetId, verificationCardIds);
	}

	public SetupComponentTallyDataPayload generate(final String electionEventId, final String verificationCardSetId,
			final ImmutableList<String> verificationCardIds) {
		final String ballotBoxAlias = uuidGenerator.generate();

		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys = verificationCardIds.stream()
				.map(verificationCardId -> elGamalGenerator.genRandomPublicKey(1))
				.collect(GroupVector.toGroupVector());

		final SetupComponentTallyDataPayload setupComponentVerificationDataPayload = new SetupComponentTallyDataPayload(encryptionGroup,
				electionEventId, verificationCardSetId, verificationCardIds, ballotBoxAlias, verificationCardPublicKeys);

		final ImmutableByteArray payloadHash = hash.recursiveHash(setupComponentVerificationDataPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		setupComponentVerificationDataPayload.setSignature(signature);

		return setupComponentVerificationDataPayload;
	}
}
