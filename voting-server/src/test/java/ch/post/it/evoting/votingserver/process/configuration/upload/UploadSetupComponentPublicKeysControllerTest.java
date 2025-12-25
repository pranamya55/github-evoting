/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.internal.hashing.HashService;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.idempotence.IdempotentExecutionRepository;
import ch.post.it.evoting.votingserver.process.SetupComponentPublicKeysService;

@DisplayName("UploadSetupComponentPublicKeysController")
class UploadSetupComponentPublicKeysControllerTest {

	private static final SetupComponentPublicKeysService SETUP_COMPONENT_PUBLIC_KEYS_SERVICE = mock(SetupComponentPublicKeysService.class);
	private static final IdempotentExecutionRepository IDEMPOTENT_EXECUTION_REPOSITORY = mock(IdempotentExecutionRepository.class);
	private static final IdempotenceService<IdempotenceContext> IDEMPOTENCE_SERVICE = new IdempotenceService<>(HashService.getInstance(),
			IDEMPOTENT_EXECUTION_REPOSITORY
	);
	private static SetupComponentPublicKeysPayload setupComponentPublicKeysPayload;
	private static String electionEventId;
	private static UploadSetupComponentPublicKeysController uploadSetupComponentPublicKeysController;

	@BeforeAll
	static void setUpAll() {
		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator();
		setupComponentPublicKeysPayload = setupComponentPublicKeysPayloadGenerator.generate();

		uploadSetupComponentPublicKeysController = new UploadSetupComponentPublicKeysController(IDEMPOTENCE_SERVICE,
				SETUP_COMPONENT_PUBLIC_KEYS_SERVICE);

		electionEventId = setupComponentPublicKeysPayload.getElectionEventId();
	}

	@Test
	@DisplayName("save setup component public keys with valid parameters")
	void saveSetupComponentPublicKeysHappyPath() {
		when(IDEMPOTENT_EXECUTION_REPOSITORY.existsById(any())).thenReturn(false);
		when(IDEMPOTENT_EXECUTION_REPOSITORY.save(any())).thenReturn(null);

		uploadSetupComponentPublicKeysController.upload(electionEventId, setupComponentPublicKeysPayload);

		verify(SETUP_COMPONENT_PUBLIC_KEYS_SERVICE, times(1)).save(any());
	}

}
