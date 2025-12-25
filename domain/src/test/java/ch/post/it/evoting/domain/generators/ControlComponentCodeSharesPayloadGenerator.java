/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class ControlComponentCodeSharesPayloadGenerator {

	private static final Hash hash = createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	final GqGroup encryptionGroup;
	final ElGamalGenerator elGamalGenerator;
	final ZqGroupGenerator zqGroupGenerator;

	public ControlComponentCodeSharesPayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
		this.elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(encryptionGroup);
		this.zqGroupGenerator = new ZqGroupGenerator(zqGroup);
	}

	public ControlComponentCodeSharesPayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public ImmutableList<ControlComponentCodeSharesPayload> generate() {
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final int chunkId = 0;
		final int numberOfEligibleVoters = 10;
		final int numberOfVotingOptions = 3;

		return generate(electionEventId, verificationCardSetId, chunkId, numberOfEligibleVoters, numberOfVotingOptions);
	}

	public ImmutableList<ControlComponentCodeSharesPayload> generate(final String electionEventId, final String verificationCardSetId,
			final int chunkId, final int numberOfEligibleVoters, final int numberOfVotingOptions) {
		final ImmutableList<String> verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(numberOfEligibleVoters)
				.collect(toImmutableList());
		return generate(electionEventId, verificationCardSetId, chunkId, verificationCardIds, numberOfVotingOptions);
	}

	public ImmutableList<ControlComponentCodeSharesPayload> generate(final String electionEventId, final String verificationCardSetId,
			final int chunkId, final ImmutableList<String> verificationCardIds, final int numberOfVotingOptions) {
		final ImmutableList<ControlComponentCodeShare> controlComponentCodeShares = verificationCardIds.stream()
				.map(verificationCardId -> generateControlComponentCodeShare(verificationCardId, numberOfVotingOptions))
				.collect(toImmutableList());

		return ControlComponentNode.ids().stream()
				.map(nodeId -> generate(electionEventId, verificationCardSetId, nodeId, chunkId, controlComponentCodeShares))
				.collect(toImmutableList());
	}

	private ControlComponentCodeSharesPayload generate(final String electionEventId, final String verificationCardSetId, final int nodeId,
			final int chunkId, final ImmutableList<ControlComponentCodeShare> controlComponentCodeShares) {
		final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload = new ControlComponentCodeSharesPayload(electionEventId,
				verificationCardSetId, chunkId, encryptionGroup, controlComponentCodeShares, nodeId);

		final ImmutableByteArray payloadHash = hash.recursiveHash(controlComponentCodeSharesPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		controlComponentCodeSharesPayload.setSignature(signature);

		return controlComponentCodeSharesPayload;
	}

	private ControlComponentCodeShare generateControlComponentCodeShare(final String verificationCardId, final int numberOfVotingOptions) {
		final ElGamalMultiRecipientPublicKey voterChoiceReturnCodeGenerationPublicKey = elGamalGenerator.genRandomPublicKey(1);

		final ElGamalMultiRecipientPublicKey voterVoteCastReturnCodeGenerationPublicKey = elGamalGenerator.genRandomPublicKey(1);

		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(
				numberOfVotingOptions);
		final ExponentiationProof encryptedPartialChoiceReturnCodeExponentiationProof = new ExponentiationProof(
				zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember()
		);

		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedConfirmationKey = elGamalGenerator.genRandomCiphertext(1);
		final ExponentiationProof encryptedConfirmationKeyExponentiationProof = new ExponentiationProof(
				zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember()
		);

		return new ControlComponentCodeShare(verificationCardId, voterChoiceReturnCodeGenerationPublicKey, voterVoteCastReturnCodeGenerationPublicKey,
				exponentiatedEncryptedPartialChoiceReturnCodes, exponentiatedEncryptedConfirmationKey,
				encryptedPartialChoiceReturnCodeExponentiationProof, encryptedConfirmationKeyExponentiationProof);
	}
}
