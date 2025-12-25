/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;

import java.util.stream.IntStream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;

public class ControlComponentBallotBoxPayloadGenerator {

	private static final Hash hash = createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	final GqGroup encryptionGroup;
	final ZqGroup zqGroup;
	final ElGamalGenerator elGamalGenerator;
	final ZqGroupGenerator zqGroupGenerator;

	public ControlComponentBallotBoxPayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
		this.elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		this.zqGroup = ZqGroup.sameOrderAs(encryptionGroup);
		this.zqGroupGenerator = new ZqGroupGenerator(zqGroup);
	}

	public ControlComponentBallotBoxPayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public ImmutableList<ControlComponentBallotBoxPayload> generate() {
		final String electionEventId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();
		final int psi = 5;
		final int delta = 1;

		return generate(electionEventId, ballotBoxId, psi, delta);
	}

	public ImmutableList<ControlComponentBallotBoxPayload> generate(final String electionEventId, final String ballotBoxId,
			final int numberOfSelections, final int numberOfWriteInsPlusOne) {
		final String verificationCardSetId = uuidGenerator.generate();

		final int numberOfConfirmedVotes = 10;
		final ImmutableList<EncryptedVerifiableVote> encryptedVerifiableVote = IntStream.range(0, numberOfConfirmedVotes)
				.mapToObj(i -> {
					final ContextIds contextIds = new ContextIds(
							electionEventId,
							verificationCardSetId,
							uuidGenerator.generate()
					);
					return generateEncryptedVerifiableVote(contextIds, numberOfSelections, numberOfWriteInsPlusOne);
				})
				.collect(toImmutableList());

		return ControlComponentNode.ids().stream()
				.map(nodeId -> generateControlComponentBallotBoxPayload(electionEventId, ballotBoxId, nodeId, encryptedVerifiableVote))
				.collect(toImmutableList());
	}

	private ControlComponentBallotBoxPayload generateControlComponentBallotBoxPayload(final String electionEventId,
			final String ballotBoxId, final int nodeId, final ImmutableList<EncryptedVerifiableVote> encryptedVerifiableVote) {

		final ControlComponentBallotBoxPayload controlComponentBallotBoxPayload = new ControlComponentBallotBoxPayload(encryptionGroup,
				electionEventId, ballotBoxId, nodeId, encryptedVerifiableVote);

		final ImmutableByteArray payloadHash = hash.recursiveHash(controlComponentBallotBoxPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		controlComponentBallotBoxPayload.setSignature(signature);

		return controlComponentBallotBoxPayload;
	}

	public EncryptedVerifiableVote generateEncryptedVerifiableVote(final ContextIds contextIds, final int psi, final int delta) {
		final ElGamalMultiRecipientCiphertext encryptedVote = elGamalGenerator.genRandomCiphertext(delta);

		final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(psi);

		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote = elGamalGenerator.genRandomCiphertext(1);

		final ExponentiationProof exponentiationProof = new ExponentiationProof(
				zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember()
		);

		final PlaintextEqualityProof plaintextEqualityProof = new PlaintextEqualityProof(
				zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementVector(2)
		);

		return new EncryptedVerifiableVote(contextIds, encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes,
				exponentiationProof, plaintextEqualityProof);
	}
}
