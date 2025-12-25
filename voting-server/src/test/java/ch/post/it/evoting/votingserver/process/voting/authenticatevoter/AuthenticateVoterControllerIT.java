/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.VoteTexts;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.KeystoreRepository;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;
import ch.post.it.evoting.votingserver.multitenancy.TenantLookupService;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.VotingClientPublicKeys;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.process.voting.ConfirmationKeyInvalidException;
import ch.post.it.evoting.votingserver.process.voting.CredentialIdNotFoundException;
import ch.post.it.evoting.votingserver.process.voting.VerifyAuthenticationChallengeException;
import ch.post.it.evoting.votingserver.process.voting.VoterAuthenticationData;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeService;

@WebFluxTest(value = AuthenticateVoterController.class)
class AuthenticateVoterControllerIT extends TestGroupSetup {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private String electionEventId;
	private String credentialId;
	private AuthenticateVoterPayload authenticateVoterPayload;

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdentifierValidationService identifierValidationService;

	@MockitoBean
	private VerifyAuthenticationChallengeService verifyAuthenticationChallengeService;

	@MockitoBean
	private AuthenticateVoterService authenticateVoterService;

	@MockitoBean
	private ContextHolder contextHolder;

	@MockitoBean
	private TenantLookupService tenantLookupService;

	@MockitoBean
	private TenantService tenantService;

	@BeforeEach
	void setup() {
		reset(identifierValidationService, verifyAuthenticationChallengeService, authenticateVoterService);

		electionEventId = uuidGenerator.generate();
		credentialId = uuidGenerator.generate();

		authenticateVoterPayload = authenticateVoterPayload();

		when(tenantLookupService.lookupTenantFromElectionEventId(electionEventId))
				.thenReturn(Optional.of(new Tenant(TenantConstants.TEST_TENANT_ID, mock(DataSource.class), mock(KeystoreRepository.class))));
	}

	@Test
	@DisplayName("returns 200 with correct response body")
	void happyPath() throws Exception {
		doNothing().when(identifierValidationService).validateCredentialId(electionEventId, credentialId);

		doNothing()
				.when(verifyAuthenticationChallengeService)
				.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.AUTHENTICATE_VOTER,
						authenticateVoterPayload.authenticationChallenge());

		final String targetUrl = String.format(
				"/api/v1/processor/voting/authenticatevoter/electionevent/%s/credentialId/%s/authenticate", electionEventId, credentialId);

		// Request payload.
		final byte[] authenticateVoterPayloadBytes = objectMapper.writeValueAsBytes(authenticateVoterPayload);

		// Expected response payload.
		final AuthenticateVoterResponsePayload authenticateVoterResponsePayload = authenticateVoterResponsePayload();
		when(authenticateVoterService.retrieveAuthenticateVoterPayload(electionEventId, credentialId))
				.thenReturn(authenticateVoterResponsePayload);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(authenticateVoterPayloadBytes)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json(objectMapper.writeValueAsString(authenticateVoterResponsePayload));
	}

	@Test
	@DisplayName("returns 401 when verification of authentication challenge fails")
	void verifyAuthenticationChallengeFails() throws Exception {
		doNothing().when(identifierValidationService).validateCredentialId(electionEventId, credentialId);

		final int attemptsLeft = 3;
		doThrow(new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID, attemptsLeft, "errorMessage"))
				.when(verifyAuthenticationChallengeService)
				.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.AUTHENTICATE_VOTER,
						authenticateVoterPayload.authenticationChallenge());

		final String targetUrl = String.format(
				"/api/v1/processor/voting/authenticatevoter/electionevent/%s/credentialId/%s/authenticate", electionEventId, credentialId);

		// Request payload.
		final byte[] authenticateVoterPayloadBytes = objectMapper.writeValueAsBytes(authenticateVoterPayload);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(authenticateVoterPayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().consumeWith(consumer -> {
					try {
						final JsonNode response = objectMapper.readTree(consumer.getResponseBody());
						checkNotNull(response);
						assertEquals(VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID.name(), response.get("errorStatus").asText());
						assertEquals(attemptsLeft, response.get("numberOfRemainingAttempts").asInt());

						final long now = Instant.now().getEpochSecond();
						final long timestamp = response.get("timestamp").asLong();

						// Check that the timestamp is within 2 seconds of the current time.
						assertTrue(Math.abs(now - timestamp) <= 2);
					} catch (final IOException e) {
						fail(e);
					}
				});
	}

	@Test
	@DisplayName("returns 401 when verification corresponding to credential id is not found")
	void credentialIdNotFound() throws Exception {
		doNothing().when(identifierValidationService).validateCredentialId(electionEventId, credentialId);

		doThrow(new CredentialIdNotFoundException("Not found"))
				.when(verifyAuthenticationChallengeService)
				.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.AUTHENTICATE_VOTER,
						authenticateVoterPayload.authenticationChallenge());

		final String targetUrl = String.format(
				"/api/v1/processor/voting/authenticatevoter/electionevent/%s/credentialId/%s/authenticate", electionEventId, credentialId);

		// Request payload.
		final byte[] authenticateVoterPayloadBytes = objectMapper.writeValueAsBytes(authenticateVoterPayload);

		// Expected response.
		final ObjectNode errorMessageNode = objectMapper.createObjectNode();
		errorMessageNode.put("errorStatus", "START_VOTING_KEY_INVALID");

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(authenticateVoterPayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().json(errorMessageNode.toString());
	}

	@Test
	@DisplayName("returns 401 when confirmation key is invalid")
	void invalidConfirmationKey() throws Exception {
		doNothing().when(identifierValidationService).validateCredentialId(electionEventId, credentialId);

		final int attemptsLeft = 4;
		doThrow(new ConfirmationKeyInvalidException("Invalid confirmation key.", attemptsLeft))
				.when(verifyAuthenticationChallengeService)
				.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.AUTHENTICATE_VOTER,
						authenticateVoterPayload.authenticationChallenge());

		final String targetUrl = String.format(
				"/api/v1/processor/voting/authenticatevoter/electionevent/%s/credentialId/%s/authenticate", electionEventId, credentialId);

		// Request payload.
		final byte[] authenticateVoterPayloadBytes = objectMapper.writeValueAsBytes(authenticateVoterPayload);

		// Expected response payload.
		final ObjectNode errorMessageNode = objectMapper.createObjectNode();
		errorMessageNode.put("errorStatus", "CONFIRMATION_KEY_INVALID");
		errorMessageNode.put("numberOfRemainingAttempts", attemptsLeft);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(authenticateVoterPayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().json(errorMessageNode.toString());
	}

	private AuthenticateVoterPayload authenticateVoterPayload() {
		final String derivedAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance());
		final BigInteger authenticationNonce = random.genRandomInteger(TWO_POW_256);

		final AuthenticationChallenge authenticationChallenge = new AuthenticationChallenge(credentialId, derivedAuthenticationChallenge,
				authenticationNonce);

		return new AuthenticateVoterPayload(electionEventId, authenticationChallenge);
	}

	private AuthenticateVoterResponsePayload authenticateVoterResponsePayload() {
		final VoteTexts voteTexts = ElectionEventContextPayloadGenerator.generateVoteTexts();
		final VoterMaterial voterMaterial = new VoterMaterial(ImmutableList.of(voteTexts), ImmutableList.emptyList(),
				ImmutableList.of("1111", "2222", "3333", "4444"), "55555555");

		final VoterAuthenticationData voterAuthenticationData = new VoterAuthenticationData(electionEventId,
				uuidGenerator.generate(),
				uuidGenerator.generate(),
				uuidGenerator.generate(),
				uuidGenerator.generate(),
				credentialId);

		final VerificationCardKeystore verificationCardKeystore = new VerificationCardKeystore(uuidGenerator.generate(),
				"%s=".formatted(random.genRandomString(571, base64Alphabet)));

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final VotingClientPublicKeys votingClientPublicKeys = new VotingClientPublicKeys(gqGroup, elGamalGenerator.genRandomPublicKey(1),
				elGamalGenerator.genRandomPublicKey(1));

		final GqGroup gqGroup = GroupTestData.getGroupP59();
		final int size = 7;
		final PrimesMappingTable primesMappingTable = new PrimesMappingTableGenerator(gqGroup).generate(size);

		return new AuthenticateVoterResponsePayload(VerificationCardState.INITIAL, voterMaterial, voterAuthenticationData, verificationCardKeystore,
				votingClientPublicKeys, primesMappingTable);
	}

	@TestConfiguration
	static class Configuration {

		@Bean
		ObjectMapper authenticateVoterControllerITObjectMapper() {
			return DomainObjectMapper.getNewInstance();
		}
	}
}
