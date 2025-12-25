/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;

import java.util.stream.IntStream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.VerifiableShuffleGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptionGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class ControlComponentShufflePayloadGenerator {

	private static final Hash hash = createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private final GqGroup encryptionGroup;

	public ControlComponentShufflePayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
	}

	public ControlComponentShufflePayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public ImmutableList<ControlComponentShufflePayload> generate() {
		final String electionEventId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();
		final int numberOfMixedVotes = 10;
		final int numberOfWriteInsPlusOne = 1;

		return generate(electionEventId, ballotBoxId, numberOfMixedVotes, numberOfWriteInsPlusOne);
	}

	public ImmutableList<ControlComponentShufflePayload> generate(final String electionEventId, final String ballotBoxId,
			final int numberOfMixedVotes, final int numberOfWriteInsPlusOne) {
		return ControlComponentNode.ids().stream()
				.map(nodeId -> generate(electionEventId, ballotBoxId, nodeId, numberOfMixedVotes, numberOfWriteInsPlusOne))
				.collect(toImmutableList());
	}

	public ImmutableList<ControlComponentShufflePayload> generate(final String electionEventId, final String ballotBoxId, final int nodeId) {
		final int numberOfMixedVotes = 10;
		final int numberOfWriteInsPlusOne = 1;

		return IntStream.range(1, nodeId)
				.mapToObj(n -> generate(electionEventId, ballotBoxId, n, numberOfMixedVotes, numberOfWriteInsPlusOne))
				.collect(toImmutableList());
	}

	public ControlComponentShufflePayload generate(final int nodeId, final int numberOfMixedVotes, final int numberOfWriteInsPlusOne) {
		final String electionEventId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();

		return generate(electionEventId, ballotBoxId, nodeId, numberOfMixedVotes, numberOfWriteInsPlusOne);
	}

	public ControlComponentShufflePayload generate(final String electionEventId, final String ballotBoxId, final int nodeId,
			final int numberOfMixedVotes, final int numberOfWriteInsPlusOne) {
		final VerifiableShuffle verifiableShuffle = new VerifiableShuffleGenerator(encryptionGroup).genVerifiableShuffle(numberOfMixedVotes,
				numberOfWriteInsPlusOne);

		final VerifiableDecryptions verifiableDecryptions = new VerifiableDecryptionGenerator(encryptionGroup).genVerifiableDecryption(
				numberOfMixedVotes, numberOfWriteInsPlusOne);

		final ControlComponentShufflePayload controlComponentShufflePayload = new ControlComponentShufflePayload(encryptionGroup, electionEventId,
				ballotBoxId, nodeId, verifiableShuffle, verifiableDecryptions);

		final ImmutableByteArray payloadHash = hash.recursiveHash(controlComponentShufflePayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		controlComponentShufflePayload.setSignature(signature);

		return controlComponentShufflePayload;
	}
}
