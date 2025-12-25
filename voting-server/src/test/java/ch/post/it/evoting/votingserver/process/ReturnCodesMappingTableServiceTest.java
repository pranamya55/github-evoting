/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

@DisplayName("ReturnCodesMappingTableService")
class ReturnCodesMappingTableServiceTest {

	private static final ObjectMapper objectMapper = spy(DomainObjectMapper.getNewInstance());
	private static final VerificationCardSetService verificationCardSetService = mock(VerificationCardSetService.class);
	private static final ReturnCodesMappingTableRepository returnCodesMappingTableEntryRepository = mock(
			ReturnCodesMappingTableRepository.class);

	private static ReturnCodesMappingTableService returnCodesMappingTableService;

	@BeforeAll
	static void setUpAll() {
		returnCodesMappingTableService = new ReturnCodesMappingTableService(verificationCardSetService, returnCodesMappingTableEntryRepository, 2);
	}

	@AfterEach
	void tearDown() {
		reset(objectMapper, verificationCardSetService, returnCodesMappingTableEntryRepository);
	}

	@Test
	@DisplayName("Saving with null parameter throws NullPointerException")
	void savingNullThrows() {
		assertThrows(NullPointerException.class, () -> returnCodesMappingTableService.save(null));
	}

}
