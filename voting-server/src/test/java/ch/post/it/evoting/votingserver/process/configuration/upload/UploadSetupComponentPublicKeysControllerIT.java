/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.KeystoreRepository;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.multitenancy.TenantLookupService;
import ch.post.it.evoting.votingserver.process.SetupComponentPublicKeysService;

@WebFluxTest(value = UploadSetupComponentPublicKeysController.class)
class UploadSetupComponentPublicKeysControllerIT extends TestGroupSetup {

	private static final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

	private String electionEventId;
	private SetupComponentPublicKeysPayload setupComponentPublicKeysPayload;

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private SetupComponentPublicKeysService setupComponentPublicKeysService;

	@MockitoBean
	private SignatureKeystore<Alias> signatureKeystore;

	@MockitoBean
	private IdempotenceService<IdempotenceContext> idempotenceService;

	@MockitoBean
	private ContextHolder contextHolder;

	@MockitoBean
	private TenantLookupService tenantLookupService;

	@MockitoBean
	private TenantService tenantService;

	@BeforeEach
	void setup() {
		reset(setupComponentPublicKeysService, signatureKeystore, idempotenceService);

		final SetupComponentPublicKeysPayloadGenerator generator = new SetupComponentPublicKeysPayloadGenerator();
		setupComponentPublicKeysPayload = generator.generate();
		electionEventId = setupComponentPublicKeysPayload.getElectionEventId();

		when(tenantLookupService.lookupTenantFromElectionEventId(electionEventId))
				.thenReturn(Optional.of(new Tenant(TenantConstants.TEST_TENANT_ID, mock(DataSource.class), mock(KeystoreRepository.class))));
	}

	@Test
	@DisplayName("save returns 200 with correct response body")
	void happyPathSave() throws Exception {
		doNothing().when(idempotenceService).execute(any(), any(), any(), any());

		doNothing().when(setupComponentPublicKeysService).save(any());

		when(signatureKeystore.verifySignature(any(), any(), any(), any())).thenReturn(true);

		final String targetUrl = String.format("/api/v1/processor/configuration/setupkeys/electionevent/%s", electionEventId);

		// Request payload.
		final byte[] setupComponentPublicKeysPayloadBytes = objectMapper.writeValueAsBytes(setupComponentPublicKeysPayload);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(setupComponentPublicKeysPayloadBytes)
				.exchange()
				.expectStatus().isOk();
	}

	@TestConfiguration
	static class Configuration {

		@Bean
		ObjectMapper setupComponentPublicKeysControllerITObjectMapper() {
			return DomainObjectMapper.getNewInstance();
		}
	}

}
