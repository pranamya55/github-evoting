/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

class BallotBoxEntityTest {

	@Test
	void constructWithoutParametersDoesNotThrow() {
		final BallotBoxEntity ballotBoxEntity = new BallotBoxEntity();
		assertFalse(ballotBoxEntity.isMixed());
		assertFalse(ballotBoxEntity.isTestBallotBox());
	}

	@Test
	void constructWithValidParametersDoesNotThrow() throws JsonProcessingException {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String ballotBoxId = uuidGenerator.generate();
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity();

		final PrimesMappingTable primesMappingTable = new PrimesMappingTableGenerator().generate(2);

		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final BallotBoxEntity ballotBoxEntity = new BallotBoxEntity.Builder()
				.setBallotBoxId(ballotBoxId)
				.setVerificationCardSetEntity(verificationCardSetEntity)
				.setBallotBoxStartTime(LocalDateTimeUtils.now().minusDays(1))
				.setBallotBoxFinishTime(LocalDateTimeUtils.now().plusDays(5))
				.setTestBallotBox(true)
				.setNumberOfEligibleVoters(10).setGracePeriod(900)
				.setPrimesMappingTable(new ImmutableByteArray(objectMapper.writeValueAsBytes(primesMappingTable)))
				.build();
		assertEquals(ballotBoxId, ballotBoxEntity.getBallotBoxId());
		assertEquals(verificationCardSetEntity, ballotBoxEntity.getVerificationCardSetEntity());
		assertFalse(ballotBoxEntity.isMixed());
		assertTrue(ballotBoxEntity.isTestBallotBox());
	}
}
