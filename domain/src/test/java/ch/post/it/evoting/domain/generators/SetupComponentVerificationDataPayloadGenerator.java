/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.RandomFactory.createRandom;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class SetupComponentVerificationDataPayloadGenerator {

	private static final Hash hash = createHash();
	private static final Random random = createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private final GqGroup encryptionGroup;
	private final ElGamalGenerator elGamalGenerator;

	public SetupComponentVerificationDataPayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
		this.elGamalGenerator = new ElGamalGenerator(encryptionGroup);
	}

	public SetupComponentVerificationDataPayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public SetupComponentVerificationDataPayload generate() {
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final int numberOfVotingOptions = 3;

		return generate(electionEventId, verificationCardSetId, numberOfVotingOptions);
	}

	public SetupComponentVerificationDataPayload generate(final String electionEventId, final String verificationCardSetId,
			final int numberOfVotingOptions) {
		final int chunkId = 0;
		final int numberOfEligibleVoters = 10;
		return generate(electionEventId, verificationCardSetId, chunkId, numberOfEligibleVoters, numberOfVotingOptions);
	}

	public SetupComponentVerificationDataPayload generate(final int chunkId, final int numberOfEligibleVoters, final int numberOfVotingOptions) {
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		return generate(electionEventId, verificationCardSetId, chunkId, numberOfEligibleVoters, numberOfVotingOptions);
	}

	public SetupComponentVerificationDataPayload generate(final String electionEventId, final int chunkId, final int numberOfEligibleVoters,
			final int numberOfVotingOptions) {
		final String verificationCardSetId = uuidGenerator.generate();
		return generate(electionEventId, verificationCardSetId, chunkId, numberOfEligibleVoters, numberOfVotingOptions);
	}

	public SetupComponentVerificationDataPayload generate(final String electionEventId, final String verificationCardSetId, final int chunkId,
			final int numberOfEligibleVoters, final int numberOfVotingOptions) {
		final ImmutableList<String> verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(numberOfEligibleVoters)
				.collect(toImmutableList());
		return generate(electionEventId, verificationCardSetId, chunkId, verificationCardIds, numberOfVotingOptions);
	}

	public SetupComponentVerificationDataPayload generate(final String electionEventId, final String verificationCardSetId, final int chunkId,
			final ImmutableList<String> verificationCardIds, final int numberOfVotingOptions) {
		final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData = verificationCardIds.stream()
				.map(verificationCardId -> generateSetupComponentVerificationData(verificationCardId, numberOfVotingOptions))
				.collect(toImmutableList());

		final int numberOfEligibleVoters = verificationCardIds.size();
		final ImmutableList<String> partialChoiceReturnCodesAllowList = Stream.generate(
						() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit((long) numberOfVotingOptions * numberOfEligibleVoters)
				.sorted()
				.collect(toImmutableList());

		final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload = new SetupComponentVerificationDataPayload(encryptionGroup,
				electionEventId, verificationCardSetId, chunkId, partialChoiceReturnCodesAllowList, setupComponentVerificationData);

		final ImmutableByteArray payloadHash = hash.recursiveHash(setupComponentVerificationDataPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		setupComponentVerificationDataPayload.setSignature(signature);

		return setupComponentVerificationDataPayload;
	}

	private SetupComponentVerificationData generateSetupComponentVerificationData(final String verificationCardId,
			final int totalNumberOfVotingOptions) {
		final ElGamalMultiRecipientCiphertext encryptedHashedSquaredPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(
				totalNumberOfVotingOptions);

		final ElGamalMultiRecipientCiphertext encryptedHashedSquaredConfirmationKey = elGamalGenerator.genRandomCiphertext(1);

		final ElGamalMultiRecipientPublicKey verificationCardPublicKey = elGamalGenerator.genRandomPublicKey(1);

		return new SetupComponentVerificationData(verificationCardId, verificationCardPublicKey, encryptedHashedSquaredPartialChoiceReturnCodes,
				encryptedHashedSquaredConfirmationKey);
	}
}
