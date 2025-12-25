/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
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
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.KeystoreRepository;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionCompletableFuture;
import ch.post.it.evoting.votingserver.multitenancy.TenantLookupService;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.process.voting.ConfirmationKeyInvalidException;
import ch.post.it.evoting.votingserver.process.voting.CredentialIdNotFoundException;
import ch.post.it.evoting.votingserver.process.voting.VerifyAuthenticationChallengeException;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeService;

@WebFluxTest(value = SendVoteController.class)
class SendVoteControllerIT extends TestGroupSetup {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private String electionEventId;
	private String verificationCardSetId;
	private String credentialId;
	private String verificationCardId;
	private ContextIds contextIds;
	private SendVotePayload sendVotePayload;

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdentifierValidationService identifierValidationService;

	@MockitoBean
	private VerifyAuthenticationChallengeService verifyAuthenticationChallengeService;

	@MockitoBean
	private ChoiceReturnCodesService choiceReturnCodesService;

	@MockitoBean
	private ContextHolder contextHolder;

	@MockitoBean
	private TenantLookupService tenantLookupService;

	@MockitoBean
	private TenantService tenantService;

	@BeforeEach
	void setup() {
		reset(identifierValidationService, verifyAuthenticationChallengeService, choiceReturnCodesService);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		credentialId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();
		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);

		sendVotePayload = createSendVotePayload();

		when(tenantLookupService.lookupTenantFromElectionEventId(electionEventId))
				.thenReturn(Optional.of(new Tenant(TenantConstants.TEST_TENANT_ID, mock(DataSource.class), mock(KeystoreRepository.class))));
	}

	@Test
	@DisplayName("returns 200 with correct response body")
	void happyPath() throws Exception {
		doNothing().when(identifierValidationService).validateContextIdsAndCredentialId(contextIds, credentialId);

		doNothing()
				.when(verifyAuthenticationChallengeService)
				.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.SEND_VOTE, sendVotePayload.authenticationChallenge());

		final ImmutableList<String> shortChoiceReturnCodes = ImmutableList.of("1111", "2222", "3333", "4444");
		final ResponseCompletionCompletableFuture<ImmutableList<String>> future = new ResponseCompletionCompletableFuture<>(120);
		future.complete(shortChoiceReturnCodes);

		when(choiceReturnCodesService.retrieveShortChoiceReturnCodes(contextIds, credentialId, sendVotePayload.encryptedVerifiableVote()))
				.thenReturn(future);

		final String targetUrl = String.format(
				"/api/v1/processor/voting/sendvote/electionevent/%s/verificationcardset/%s/credentialId/%s/verificationcard/%s",
				electionEventId, verificationCardSetId, credentialId, verificationCardId);

		// Request payload.
		final byte[] sendVotePayloadBytes = objectMapper.writeValueAsBytes(sendVotePayload);

		// Expected response payload.
		final SendVoteResponsePayload sendVoteResponsePayload = new SendVoteResponsePayload(shortChoiceReturnCodes);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(sendVotePayloadBytes)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json(objectMapper.writeValueAsString(sendVoteResponsePayload));
	}

	@Test
	@DisplayName("returns 401 when verification of authentication challenge fails")
	void verifyAuthenticationChallengeFails() throws Exception {
		doNothing().when(identifierValidationService).validateContextIdsAndCredentialId(contextIds, credentialId);

		doThrow(new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID, 3, "errorMessage"))
				.when(verifyAuthenticationChallengeService)
				.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.SEND_VOTE, sendVotePayload.authenticationChallenge());

		final String targetUrl = String.format(
				"/api/v1/processor/voting/sendvote/electionevent/%s/verificationcardset/%s/credentialId/%s/verificationcard/%s",
				electionEventId, verificationCardSetId, credentialId, verificationCardId);

		// Request payload.
		final byte[] sendVotePayloadBytes = objectMapper.writeValueAsBytes(sendVotePayload);

		// Expected response payload.
		final int attemptsLeft = 3;

		final String responseBody = webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(sendVotePayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();

		// Assert the response
		final JsonNode jsonNode = objectMapper.readTree(responseBody);
		assertTrue(jsonNode.get("errorStatus").asText().matches(VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID.name()));
		assertEquals(attemptsLeft, jsonNode.get("numberOfRemainingAttempts").asInt());
		assertTrue(jsonNode.get("timestamp").asText().matches("^\\d{10}$"));
	}

	@Test
	@DisplayName("returns 401 when verification corresponding to credential id is not found")
	void credentialIdNotFound() throws Exception {
		doNothing().when(identifierValidationService).validateContextIdsAndCredentialId(contextIds, credentialId);

		doThrow(new CredentialIdNotFoundException("Not found"))
				.when(verifyAuthenticationChallengeService)
				.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.SEND_VOTE, sendVotePayload.authenticationChallenge());

		final String targetUrl = String.format(
				"/api/v1/processor/voting/sendvote/electionevent/%s/verificationcardset/%s/credentialId/%s/verificationcard/%s",
				electionEventId, verificationCardSetId, credentialId, verificationCardId);

		// Request payload.
		final byte[] sendVotePayloadBytes = objectMapper.writeValueAsBytes(sendVotePayload);

		// Expected response payload.
		final ObjectNode errorMessageNode = objectMapper.createObjectNode();
		errorMessageNode.put("errorStatus", "START_VOTING_KEY_INVALID");

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(sendVotePayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().json(errorMessageNode.toString());
	}

	@Test
	@DisplayName("returns 401 when confirmation key is invalid")
	void invalidConfirmationKey() throws Exception {
		doNothing().when(identifierValidationService).validateContextIdsAndCredentialId(contextIds, credentialId);

		doThrow(new ConfirmationKeyInvalidException("Invalid confirmation key.", 4))
				.when(verifyAuthenticationChallengeService)
				.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.SEND_VOTE, sendVotePayload.authenticationChallenge());

		final String targetUrl = String.format(
				"/api/v1/processor/voting/sendvote/electionevent/%s/verificationcardset/%s/credentialId/%s/verificationcard/%s",
				electionEventId, verificationCardSetId, credentialId, verificationCardId);

		// Request payload.
		final byte[] sendVotePayloadBytes = objectMapper.writeValueAsBytes(sendVotePayload);

		// Expected response payload.
		final ObjectNode errorMessageNode = objectMapper.createObjectNode();
		errorMessageNode.put("errorStatus", "CONFIRMATION_KEY_INVALID");
		errorMessageNode.put("numberOfRemainingAttempts", 4);

		webTestClient.post().uri(targetUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(sendVotePayloadBytes)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().json(errorMessageNode.toString());
	}

	private SendVotePayload createSendVotePayload() {
		final String derivedAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final BigInteger authenticationNonce = random.genRandomInteger(TWO_POW_256);

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final EncryptedVerifiableVote encryptedVerifiableVote = genEncryptedVerifiableVote(elGamalGenerator, contextIds);
		final AuthenticationChallenge authenticationChallenge = new AuthenticationChallenge(credentialId, derivedAuthenticationChallenge,
				authenticationNonce);

		return new SendVotePayload(contextIds, gqGroup, encryptedVerifiableVote, authenticationChallenge);
	}

	private EncryptedVerifiableVote genEncryptedVerifiableVote(final ElGamalGenerator elGamalGenerator, final ContextIds contextIds) {
		final int numberOfWriteInsPlusOne = 1;
		final ElGamalMultiRecipientCiphertext encryptedVote = elGamalGenerator.genRandomCiphertext(numberOfWriteInsPlusOne);
		final GqGroup encryptionGroup = encryptedVote.getGroup();
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(encryptionGroup);
		final BigInteger exponentValue = RandomFactory.createRandom().genRandomInteger(encryptionGroup.getQ());
		final ZqElement exponent = ZqElement.create(exponentValue, zqGroup);
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote = encryptedVote.getCiphertextExponentiation(exponent);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);
		final ExponentiationProof exponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember());
		final PlaintextEqualityProof plaintextEqualityProof = new PlaintextEqualityProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementVector(2));
		final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(numberOfWriteInsPlusOne);

		return new EncryptedVerifiableVote(contextIds, encryptedVote, encryptedPartialChoiceReturnCodes, exponentiatedEncryptedVote,
				exponentiationProof, plaintextEqualityProof);
	}

	@TestConfiguration
	static class Configuration {

		@Bean
		ObjectMapper objectMapper() {
			// Override default object mapper instantiated when using WebTestClient.
			return DomainObjectMapper.getNewInstance();
		}
	}
}
