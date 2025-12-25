/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.KeystoreRepository;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.multitenancy.TenantLookupService;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.process.voting.ConfirmationKeyInvalidException;
import ch.post.it.evoting.votingserver.process.voting.CredentialIdNotFoundException;
import ch.post.it.evoting.votingserver.process.voting.VerifyAuthenticationChallengeException;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeService;

@WebFluxTest(value = ConfirmVoteController.class)
@MockitoBean(types = {
		ContextHolder.class,
		TenantService.class,
		ConfirmVoteService.class,
		IdempotenceService.class,
		TenantLookupService.class,
		VerificationCardService.class,
		IdentifierValidationService.class,
		VerifyAuthenticationChallengeService.class
})
class ConfirmVoteControllerIT extends TestGroupSetup {

	private static final Random random = RandomFactory.createRandom();
	private static final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private String credentialId;
	private ContextIds contextIds;
	private ConfirmVotePayload confirmVotePayload;
	private String targetUrl;

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private IdempotenceService<IdempotenceContext> idempotenceService;

	@Autowired
	private IdentifierValidationService identifierValidationService;

	@Autowired
	private VerifyAuthenticationChallengeService verifyAuthenticationChallengeService;

	@Autowired
	private TenantLookupService tenantLookupService;

	@Autowired
	private ConfirmVoteService confirmVoteService;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		credentialId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		confirmVotePayload = confirmVotePayload();

		targetUrl = String.format(
				"/api/v1/processor/voting/confirmvote/electionevent/%s/verificationcardset/%s/credentialId/%s/verificationcard/%s",
				electionEventId, verificationCardSetId, credentialId, verificationCardId);

		when(tenantLookupService.lookupTenantFromElectionEventId(electionEventId))
				.thenReturn(Optional.of(new Tenant(TenantConstants.TEST_TENANT_ID, mock(DataSource.class), mock(KeystoreRepository.class))));

		doNothing()
				.when(identifierValidationService)
				.validateContextIdsAndCredentialId(contextIds, credentialId);
		when(confirmVoteService.getShortVoteCastReturnCode(contextIds, credentialId))
				.thenReturn(CompletableFuture.completedFuture("123456"));
	}

	@Test
	@DisplayName("returns 200 with correct response body")
	void happyPath() throws Exception {
		final String shortVoteCastReturnCode = "55555555";
		final CompletableFuture<String> future = CompletableFuture.completedFuture(shortVoteCastReturnCode);

		doReturn(future)
				.when(idempotenceService)
				.execute(eq(IdempotenceContext.CONFIRM_VOTE), any(), any(), any(), any());

		// Request payload.
		final byte[] confirmVotePayloadBytes = objectMapper.writeValueAsBytes(confirmVotePayload);

		// Expected response payload.
		final ConfirmVoteResponsePayload confirmVoteResponsePayload = new ConfirmVoteResponsePayload(shortVoteCastReturnCode);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(confirmVotePayloadBytes)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json(objectMapper.writeValueAsString(confirmVoteResponsePayload));
	}

	@Test
	@DisplayName("returns 401 when verification of authentication challenge fails")
	void verifyAuthenticationChallengeFails() throws Exception {
		doThrow(new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID, 3, "errorMessage"))
				.when(idempotenceService)
				.execute(eq(IdempotenceContext.CONFIRM_VOTE), any(), any(), any(), any());

		// Request payload.
		final byte[] confirmVotePayloadBytes = objectMapper.writeValueAsBytes(confirmVotePayload);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(confirmVotePayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.consumeWith(consumer -> {
					try {
						final JsonNode jsonNode = objectMapper.readTree(consumer.getResponseBody());
						checkNotNull(jsonNode);

						assertEquals(VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID.name(), jsonNode.get("errorStatus").asText());
						assertEquals(3, jsonNode.get("numberOfRemainingAttempts").asInt());

						final long now = Instant.now().getEpochSecond();
						final long timestamp = jsonNode.get("timestamp").asLong();

						// The timestamp should be within 2 seconds of the current time.
						assertTrue(Math.abs(now - timestamp) <= 2);
					} catch (final IOException e) {
						fail(e);
					}
				});
	}

	@Test
	@DisplayName("returns 401 when verification corresponding to credential id is not found")
	void credentialIdNotFound() throws Exception {
		doThrow(new CredentialIdNotFoundException("Not found"))
				.when(idempotenceService)
				.execute(eq(IdempotenceContext.CONFIRM_VOTE), any(), any(), any(), any());

		// Request payload.
		final byte[] confirmVotePayloadBytes = objectMapper.writeValueAsBytes(confirmVotePayload);

		// Expected response payload.
		final ObjectNode errorMessageNode = objectMapper.createObjectNode();
		errorMessageNode.put("errorStatus", "START_VOTING_KEY_INVALID");

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(confirmVotePayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().json(errorMessageNode.toString());
	}

	@Test
	@DisplayName("returns 401 when confirmation key is invalid")
	void invalidConfirmationKey() throws Exception {
		doThrow(new ConfirmationKeyInvalidException("Invalid confirmation key.", 4))
				.when(idempotenceService)
				.execute(eq(IdempotenceContext.CONFIRM_VOTE), any(), any(), any(), any());
		// Request payload.
		final byte[] confirmVotePayloadBytes = objectMapper.writeValueAsBytes(confirmVotePayload);

		// Expected response payload.
		final ObjectNode errorMessageNode = objectMapper.createObjectNode();
		errorMessageNode.put("errorStatus", "CONFIRMATION_KEY_INVALID");
		errorMessageNode.put("numberOfRemainingAttempts", 4);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(confirmVotePayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().json(errorMessageNode.toString());
	}

	private ConfirmVotePayload confirmVotePayload() {
		final String derivedAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final BigInteger authenticationNonce = random.genRandomInteger(TWO_POW_256);

		final GqElement confirmationKey = gqGroupGenerator.genMember();
		final AuthenticationChallenge authenticationChallenge = new AuthenticationChallenge(credentialId, derivedAuthenticationChallenge,
				authenticationNonce);

		return new ConfirmVotePayload(contextIds, gqGroup, confirmationKey, authenticationChallenge);
	}

}
