/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.securedatamanager.shared.process.BoardPasswordHashService;

@ExtendWith(MockitoExtension.class)
class ElectoralBoardConfigServiceTest {

	private static final String ELECTION_EVENT_ID = "0B149CFDAAD04B04B990C3B1D4CA7639";
	private static final String ELECTORAL_BOARD_ID = "16E020D934594544A6E17D1E410DA513";

	private static final BoardPasswordHashService boardPasswordHashServiceMock = mock(BoardPasswordHashService.class);
	private static final ElectoralBoardConstitutionService constituteService = mock(ElectoralBoardConstitutionService.class);
	private static final ImmutableList<SafePasswordHolder> ELECTORAL_BOARD_PASSWORDS = ImmutableList.of(
			new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()),
			new SafePasswordHolder("Password_ElectoralBoard2".toCharArray()));


	private static ElectoralBoardConfigService electoralBoardConfigService;

	@BeforeAll
	static void setUp() {
		electoralBoardConfigService = new ElectoralBoardConfigService(
				boardPasswordHashServiceMock,
				constituteService);
	}

	@Test
	void constituteHappyPath() {
		assertDoesNotThrow(() -> electoralBoardConfigService.constitute(ELECTION_EVENT_ID, ELECTORAL_BOARD_ID, ELECTORAL_BOARD_PASSWORDS));
	}
}
