/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BCK_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

class VoterInitialCodesPayloadTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private String electionEventId;
	private String verificationCardSetId;
	private ImmutableList<VoterInitialCodes> voterInitialCodesList;

	@BeforeEach
	void setup() {
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		voterInitialCodesList = Stream.generate(this::generateVoterInitialCodes).limit(5).collect(toImmutableList());
	}

	@Test
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> new VoterInitialCodesPayload(null, verificationCardSetId, voterInitialCodesList));
		assertThrows(NullPointerException.class, () -> new VoterInitialCodesPayload(electionEventId, null, voterInitialCodesList));
		assertThrows(NullPointerException.class, () -> new VoterInitialCodesPayload(electionEventId, verificationCardSetId, null));
	}

	@Test
	void constructWithEmptyVoterInitialCodesListThrows() {
		final ImmutableList<VoterInitialCodes> emptyList = ImmutableList.emptyList();
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodesPayload(electionEventId, verificationCardSetId, emptyList));
		assertEquals("The voter initial codes list must not be empty.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	void constructWithDuplicateVoterIdentificationThrows() {
		final VoterInitialCodes newVoterInitialCodes = generateVoterInitialCodes();
		final VoterInitialCodes voterInitialCodesDuplicateVoterIdentification = new VoterInitialCodes(
				voterInitialCodesList.get(0).voterIdentification(),
				newVoterInitialCodes.votingCardId(), newVoterInitialCodes.verificationCardId(), newVoterInitialCodes.startVotingKey(),
				newVoterInitialCodes.extendedAuthenticationFactor(), newVoterInitialCodes.ballotCastingKey());
		final ImmutableList<VoterInitialCodes> voterInitialCodesWithDuplicateVoterIdentification = voterInitialCodesList.append(
				voterInitialCodesDuplicateVoterIdentification);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodesPayload(electionEventId, verificationCardSetId, voterInitialCodesWithDuplicateVoterIdentification));
		assertEquals("The list of voter initial codes must not contain any duplicate voter identifications.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	void constructWithDuplicateVotingCardIdThrows() {
		final VoterInitialCodes newVoterInitialCodes = generateVoterInitialCodes();
		final VoterInitialCodes voterInitialCodesDuplicateVotingCardId = new VoterInitialCodes(newVoterInitialCodes.voterIdentification(),
				voterInitialCodesList.get(0).votingCardId(), newVoterInitialCodes.verificationCardId(), newVoterInitialCodes.startVotingKey(),
				newVoterInitialCodes.extendedAuthenticationFactor(), newVoterInitialCodes.ballotCastingKey());
		final ImmutableList<VoterInitialCodes> voterInitialCodesWithDuplicateVotingCardId = voterInitialCodesList.append(
				voterInitialCodesDuplicateVotingCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodesPayload(electionEventId, verificationCardSetId, voterInitialCodesWithDuplicateVotingCardId));
		assertEquals("The list of voter initial codes must not contain any duplicate voting card ids.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	void constructWithDuplicateVerificationCardIdThrows() {
		final VoterInitialCodes newVoterInitialCodes = generateVoterInitialCodes();
		final VoterInitialCodes voterInitialCodesDuplicateVerificationCardId = new VoterInitialCodes(newVoterInitialCodes.voterIdentification(),
				newVoterInitialCodes.votingCardId(), voterInitialCodesList.get(0).verificationCardId(), newVoterInitialCodes.startVotingKey(),
				newVoterInitialCodes.extendedAuthenticationFactor(), newVoterInitialCodes.ballotCastingKey());
		final ImmutableList<VoterInitialCodes> voterInitialCodesWithDuplicateVerificationCardId = voterInitialCodesList.append(
				voterInitialCodesDuplicateVerificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodesPayload(electionEventId, verificationCardSetId, voterInitialCodesWithDuplicateVerificationCardId));
		assertEquals("The list of voter initial codes must not contain any duplicate verification card ids.",
				Throwables.getRootCause(exception).getMessage());
	}

	private VoterInitialCodes generateVoterInitialCodes() {
		final String voterIdentification = random.genRandomString(50, base64Alphabet);
		final String votingCardId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		final String startVotingKey = ElectionSetupUtils.genStartVotingKey();
		final String extendedAuthenticationFactor = String.join("", random.genUniqueDecimalStrings(4, 2));
		final String ballotCastingKey = random.genUniqueDecimalStrings(BCK_LENGTH, 1).get(0);
		return new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor,
				ballotCastingKey);
	}
}
