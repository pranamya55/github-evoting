/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class ControlComponentPartialDecryptPayloadGenerator {

	private static final Hash hash = createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	final GqGroup encryptionGroup;
	final ZqGroup zqGroup;
	final GqGroupGenerator gqGroupGenerator;
	final ZqGroupGenerator zqGroupGenerator;

	public ControlComponentPartialDecryptPayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
		this.gqGroupGenerator = new GqGroupGenerator(encryptionGroup);
		this.zqGroup = ZqGroup.sameOrderAs(encryptionGroup);
		this.zqGroupGenerator = new ZqGroupGenerator(zqGroup);
	}

	public ControlComponentPartialDecryptPayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public ImmutableList<ControlComponentPartialDecryptPayload> generate() {
		final ContextIds contextIds = new ContextIds(uuidGenerator.generate(), uuidGenerator.generate(), uuidGenerator.generate());
		final int numberOfSelections = 10;

		return generate(contextIds, numberOfSelections);
	}

	public ImmutableList<ControlComponentPartialDecryptPayload> generate(final ContextIds contextIds, final int numberOfSelections) {
		return ControlComponentNode.ids().stream()
				.map(nodeId -> generate(contextIds, nodeId, numberOfSelections))
				.collect(toImmutableList());
	}

	public ControlComponentPartialDecryptPayload generate(final ContextIds contextIds, final int nodeId, final int numberOfSelections) {
		final ControlComponentPartialDecryptPayload controlComponentPartialDecryptPayload = new ControlComponentPartialDecryptPayload(encryptionGroup,
				generatePartiallyDecryptedEncryptedPCC(contextIds, nodeId, numberOfSelections));

		final ImmutableByteArray payloadHash = hash.recursiveHash(controlComponentPartialDecryptPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		controlComponentPartialDecryptPayload.setSignature(signature);

		return controlComponentPartialDecryptPayload;
	}

	/**
	 * The generated {@link ExponentiationProof} are not valid.
	 */
	private PartiallyDecryptedEncryptedPCC generatePartiallyDecryptedEncryptedPCC(final ContextIds contextIds, final int nodeId,
			final int numberOfSelections) {
		final GroupVector<GqElement, GqGroup> exponentiatedGammas = gqGroupGenerator.genRandomGqElementVector(numberOfSelections);

		final GroupVector<ExponentiationProof, ZqGroup> exponentiationProofs = Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
								zqGroupGenerator.genRandomZqElementMember()))
				.limit(numberOfSelections)
				.collect(toGroupVector());

		return new PartiallyDecryptedEncryptedPCC(contextIds, nodeId, exponentiatedGammas, exponentiationProofs);
	}
}
