/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;

@DisplayName("ExtractVCCInput constructed with")
class ExtractVCCInputTest extends TestGroupSetup {

	private ReturnCodesMappingTable returnCodesMappingTable;
	private GroupVector<GqElement, GqGroup> longVoteCastReturnCodeShares;

	@BeforeEach
	void setup() {
		longVoteCastReturnCodeShares = gqGroupGenerator.genRandomGqElementVector(Constants.NUMBER_OF_CONTROL_COMPONENTS);
		returnCodesMappingTable = Optional::of;
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void nullParamsThrows() {
		assertThrows(NullPointerException.class, () -> new ExtractVCCInput(null, returnCodesMappingTable));
		assertThrows(NullPointerException.class, () -> new ExtractVCCInput(longVoteCastReturnCodeShares, null));
	}

	@Test
	@DisplayName("wrong number of long Vote Cast Return Code shares throws IllegalArgumentException")
	void wrongNumberOfLongVoteCastReturnCodeSharesThrows() {
		// Too few.
		final GroupVector<GqElement, GqGroup> emptyShares = GroupVector.of();
		assertThrows(IllegalArgumentException.class, () -> new ExtractVCCInput(emptyShares, returnCodesMappingTable));

		// Too many.
		final GroupVector<GqElement, GqGroup> tooManyShares = longVoteCastReturnCodeShares.append(gqGroupGenerator.genMember());
		assertThrows(IllegalArgumentException.class, () -> new ExtractVCCInput(tooManyShares, returnCodesMappingTable));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		final ExtractVCCInput extractVCCInput = assertDoesNotThrow(
				() -> new ExtractVCCInput(longVoteCastReturnCodeShares, returnCodesMappingTable));
		assertEquals(longVoteCastReturnCodeShares, extractVCCInput.longVoteCastReturnCodeShares());
		assertEquals(returnCodesMappingTable, extractVCCInput.returnCodesMappingTable());
	}

}
