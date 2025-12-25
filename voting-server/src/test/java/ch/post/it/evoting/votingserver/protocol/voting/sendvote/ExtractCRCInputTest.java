/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;

@DisplayName("ExtractCRCInput constructed with")
class ExtractCRCInputTest extends TestGroupSetup {
	private static final SecureRandom secureRandom = new SecureRandom();

	private int psi;
	private ReturnCodesMappingTable returnCodesMappingTable;
	private ImmutableList<GroupVector<GqElement, GqGroup>> longChoiceReturnCodeShares;

	@BeforeEach
	void setUp() {
		psi = secureRandom.nextInt(1, 5);
		longChoiceReturnCodeShares = IntStream.range(0, Constants.NUMBER_OF_CONTROL_COMPONENTS)
				.mapToObj(i -> gqGroupGenerator.genRandomGqElementVector(psi))
				.collect(toImmutableList());
		returnCodesMappingTable = hashedLongReturnCode -> Optional.of("encryptedShortReturnCode");
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void nullParamsThrows() {
		assertThrows(NullPointerException.class, () -> new ExtractCRCInput(null, returnCodesMappingTable));
		assertThrows(NullPointerException.class, () -> new ExtractCRCInput(longChoiceReturnCodeShares, null));
	}

	@Test
	@DisplayName("wrong number of long Choice Return Code shares throws IllegalArgumentException")
	void wrongNumberOfLongChoiceReturnCodeSharesThrows() {
		// Too few.
		final ImmutableList<GroupVector<GqElement, GqGroup>> emptyShares = ImmutableList.emptyList();
		assertThrows(IllegalArgumentException.class, () -> new ExtractCRCInput(emptyShares, returnCodesMappingTable));

		// Too many.
		final ImmutableList<GroupVector<GqElement, GqGroup>> tooManyShares = longChoiceReturnCodeShares
				.append(gqGroupGenerator.genRandomGqElementVector(psi));

		assertThrows(IllegalArgumentException.class, () -> new ExtractCRCInput(tooManyShares, returnCodesMappingTable));
	}

	@Test
	@DisplayName("long Choice Return Code shares of different sizes throws IllegalArgumentException")
	void differentSizesLongChoiceReturnCodeShares() {
		final ImmutableList<GroupVector<GqElement, GqGroup>> incorrect = Stream.concat(
				Stream.of(gqGroupGenerator.genRandomGqElementVector(psi + 1)),
				longChoiceReturnCodeShares.stream().skip(1)).collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ExtractCRCInput(incorrect, returnCodesMappingTable));
		assertEquals("All long Choice Return Code Shares must have the same size.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("long Choice Return Code shares not in range throws IllegalArgumentException")
	void notInRangeLongChoiceReturnCodeShares() {
		final String errorMessage = String.format("The long Choice Return Code Shares size must be in range [1, %s].",
				MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

		// Too few.
		final ImmutableList<GroupVector<GqElement, GqGroup>> tooFewCRC = IntStream.range(0, Constants.NUMBER_OF_CONTROL_COMPONENTS)
				.mapToObj(i -> GroupVector.<GqElement, GqGroup>of())
				.collect(toImmutableList());

		final IllegalArgumentException tooFewException = assertThrows(IllegalArgumentException.class,
				() -> new ExtractCRCInput(tooFewCRC, returnCodesMappingTable));
		assertEquals(errorMessage, Throwables.getRootCause(tooFewException).getMessage());

		// Too many.
		final ImmutableList<GroupVector<GqElement, GqGroup>> tooManyCRC = IntStream.range(0, Constants.NUMBER_OF_CONTROL_COMPONENTS)
				.mapToObj(i -> gqGroupGenerator.genRandomGqElementVector(MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1))
				.collect(toImmutableList());

		final IllegalArgumentException tooManyException = assertThrows(IllegalArgumentException.class,
				() -> new ExtractCRCInput(tooManyCRC, returnCodesMappingTable));
		assertEquals(errorMessage, Throwables.getRootCause(tooManyException).getMessage());
	}

	@Test
	@DisplayName("long Choice Return Code shares of different groups throws IllegalArgumentException")
	void differentGroupsLongChoiceReturnCodeShares() {
		final ImmutableList<GroupVector<GqElement, GqGroup>> incorrect = Stream.concat(
				Stream.of(otherGqGroupGenerator.genRandomGqElementVector(psi)),
				longChoiceReturnCodeShares.stream().skip(1)
		).collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ExtractCRCInput(incorrect, returnCodesMappingTable));
		assertEquals("All long Choice Return Code Shares must have the same Gq group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		assertDoesNotThrow(() -> new ExtractCRCInput(longChoiceReturnCodeShares, returnCodesMappingTable));
	}

}