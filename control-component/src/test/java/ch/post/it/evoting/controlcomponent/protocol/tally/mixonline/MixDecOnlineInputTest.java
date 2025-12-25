/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ControlComponentVotesHashPayloadGenerator;

@DisplayName("Constructing a MixDecryptInput object with")
class MixDecOnlineInputTest extends TestGroupSetup {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes;
	private MixDecOnlineInput.Builder mixDecOnlineInputBuilder;

	@BeforeEach
	void setup() {
		final int n = SECURE_RANDOM.nextInt(20) + 1;
		final int l = SECURE_RANDOM.nextInt(10) + 1;
		final int delta_max = SECURE_RANDOM.nextInt(12) + 1;
		partiallyDecryptedVotes = elGamalGenerator.genRandomCiphertextVector(n, l);
		final ElGamalMultiRecipientPrivateKey ccmjElectionSecretKey = elGamalGenerator.genRandomPrivateKey(delta_max);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();
		final ControlComponentVotesHashPayloadGenerator controlComponentVotesHashPayloadGenerator = new ControlComponentVotesHashPayloadGenerator();
		final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads = controlComponentVotesHashPayloadGenerator.generate(
				electionEventId, ballotBoxId);
		mixDecOnlineInputBuilder = new MixDecOnlineInput.Builder()
				.setPartiallyDecryptedVotes(partiallyDecryptedVotes)
				.setCcmjElectionSecretKey(ccmjElectionSecretKey)
				.setEncryptedConfirmedVotesHash(controlComponentVotesHashPayloads.get(0).getEncryptedConfirmedVotesHash())
				.setEncryptedConfirmedVotesHashes(controlComponentVotesHashPayloads.stream()
						.map(ControlComponentVotesHashPayload::getEncryptedConfirmedVotesHash)
						.collect(toImmutableList()));
	}

	@Test
	@DisplayName("null arguments throws a NullPointerException")
	void constructWithNullArgumentsThrows() {
		final MixDecOnlineInput.Builder nullPartiallyDecryptedVotes = mixDecOnlineInputBuilder.setPartiallyDecryptedVotes(null);
		assertThrows(NullPointerException.class, nullPartiallyDecryptedVotes::build);

		final MixDecOnlineInput.Builder nullCcmjElectionSecretKey = mixDecOnlineInputBuilder.setCcmjElectionSecretKey(null);
		assertThrows(NullPointerException.class, nullCcmjElectionSecretKey::build);

		final MixDecOnlineInput.Builder nullEncryptedConfirmedVotesHash = mixDecOnlineInputBuilder.setEncryptedConfirmedVotesHash(null);
		assertThrows(NullPointerException.class, nullEncryptedConfirmedVotesHash::build);

		final MixDecOnlineInput.Builder nullEncryptedConfirmedVotesHashes = mixDecOnlineInputBuilder.setEncryptedConfirmedVotesHashes(null);
		assertThrows(NullPointerException.class, nullEncryptedConfirmedVotesHashes::build);
	}

	@Test
	@DisplayName("the partially decrypted votes having a different group than the public keys throws an IllegalArgumentException")
	void constructWithPartiallyDecryptedVotesDifferentGroupThanPublicKeysThrows() {
		final int n = partiallyDecryptedVotes.size();
		final int l = partiallyDecryptedVotes.getElementSize();
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> differentPartiallyDecryptedVotes = otherGroupElGamalGenerator.genRandomCiphertextVector(
				n, l);
		final MixDecOnlineInput.Builder builderWithDifferentPartiallyDecryptedVotes = mixDecOnlineInputBuilder.setPartiallyDecryptedVotes(
				differentPartiallyDecryptedVotes);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builderWithDifferentPartiallyDecryptedVotes::build);
		assertEquals("The partially decrypted votes must have the same group order as the CCM_j election secret key.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid inputs does not throw")
	void constructWithValidInputs() {
		assertDoesNotThrow(
				() -> mixDecOnlineInputBuilder.build());
	}
}
