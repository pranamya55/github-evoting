/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;

import java.util.stream.IntStream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class ControlComponentPublicKeysPayloadGenerator {

	private static final Hash hash = createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	final GqGroup encryptionGroup;
	final ZqGroup zqGroup;
	final ElGamalGenerator elGamalGenerator;
	final ZqGroupGenerator zqGroupGenerator;

	public ControlComponentPublicKeysPayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
		this.elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		this.zqGroup = ZqGroup.sameOrderAs(encryptionGroup);
		this.zqGroupGenerator = new ZqGroupGenerator(zqGroup);
	}

	public ControlComponentPublicKeysPayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public ImmutableList<ControlComponentPublicKeysPayload> generate() {
		final int maximumNumberOfWriteInsPlusOne = 1;
		return generate(maximumNumberOfWriteInsPlusOne);
	}

	public ImmutableList<ControlComponentPublicKeysPayload> generate(final int maximumNumberOfWriteInsPlusOne) {
		final String electionEventId = uuidGenerator.generate();
		return generate(electionEventId, maximumNumberOfWriteInsPlusOne);
	}

	public ImmutableList<ControlComponentPublicKeysPayload> generate(final String electionEventId, final int maximumNumberOfWriteInsPlusOne) {
		final int maximumNumberOfSelections = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
		return ControlComponentNode.ids().stream()
				.map(nodeId -> generateControlComponentPublicKeysPayload(electionEventId, nodeId, maximumNumberOfSelections,
						maximumNumberOfWriteInsPlusOne))
				.collect(toImmutableList());
	}

	private ControlComponentPublicKeysPayload generateControlComponentPublicKeysPayload(final String electionEventId, final int nodeId,
			final int maximumNumberOfSelections, final int maximumNumberOfWriteInsPlusOne) {

		final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload = new ControlComponentPublicKeysPayload(encryptionGroup,
				electionEventId, generateControlComponentPublicKeys(nodeId, maximumNumberOfSelections, maximumNumberOfWriteInsPlusOne));

		final ImmutableByteArray payloadHash = hash.recursiveHash(controlComponentPublicKeysPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		controlComponentPublicKeysPayload.setSignature(signature);

		return controlComponentPublicKeysPayload;
	}

	/**
	 * The generated {@link SchnorrProof} are not valid.
	 */
	private ControlComponentPublicKeys generateControlComponentPublicKeys(final int nodeId, final int maximumNumberOfSelections,
			final int maximumNumberOfWriteInsPlusOne) {

		final ElGamalMultiRecipientPublicKey ccrjChoiceReturnCodesEncryptionPublicKey = elGamalGenerator.genRandomPublicKey(
				maximumNumberOfSelections);
		final GroupVector<SchnorrProof, ZqGroup> ccrjSchnorrProofs = IntStream.range(0, maximumNumberOfSelections)
				.mapToObj(ignored -> new SchnorrProof(
						zqGroupGenerator.genRandomZqElementMember(),
						zqGroupGenerator.genRandomZqElementMember()))
				.collect(GroupVector.toGroupVector());

		final ElGamalMultiRecipientPublicKey ccmjElectionPublicKeys = elGamalGenerator.genRandomPublicKey(maximumNumberOfWriteInsPlusOne);
		final GroupVector<SchnorrProof, ZqGroup> ccmjSchnorrProofs = IntStream.range(0, maximumNumberOfWriteInsPlusOne)
				.mapToObj(ignored -> new SchnorrProof(
						zqGroupGenerator.genRandomZqElementMember(),
						zqGroupGenerator.genRandomZqElementMember()))
				.collect(GroupVector.toGroupVector());

		return new ControlComponentPublicKeys(nodeId, ccrjChoiceReturnCodesEncryptionPublicKey, ccrjSchnorrProofs, ccmjElectionPublicKeys,
				ccmjSchnorrProofs);
	}
}
