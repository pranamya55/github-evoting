/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.votingserver.multitenancy.ContextWebFilter.HEADER_TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;
import ch.post.it.evoting.votingserver.multitenancy.TenantLookupService;
import ch.post.it.evoting.votingserver.process.ElectionEventContextService;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;

@DisplayName("Given request to the Voting Card Manager API to")
@WebFluxTest(value = VotingCardManagerController.class)
class VotingCardManagerControllerIT {
	private static final String BASE_API = "/api/v1/votingcardmanager";
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static final String TENANT_ID = "it";

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ElectionEventService electionEventService;

	@MockitoBean
	private ElectionEventContextService electionEventContextService;

	@MockitoBean
	private VerificationCardService verificationCardService;

	@MockitoBean
	private ContextHolder contextHolder;

	@MockitoBean
	private TenantLookupService tenantLookupService;

	@MockitoBean
	private TenantService tenantService;

	@BeforeEach
	void setUp() {
		when(tenantService.existTenant(TENANT_ID)).thenReturn(true);
	}

	@DisplayName("search for a voting card")
	@Nested
	class Search {
		private static final int LIMIT = 5;

		@DisplayName("with a valid parameter returns successfully")
		@Test
		void searchHappyPathRest() throws JsonProcessingException {
			final String electionEvenId = uuidGenerator.generate();
			final String verificationCardSetId = uuidGenerator.generate();
			final String verificationCardId = uuidGenerator.generate();

			final ImmutableList<VotingCardDto> votingCards = IntStream.range(0, LIMIT + 1)
					.mapToObj(i -> new VotingCardDto(electionEvenId, verificationCardSetId, verificationCardId,
							uuidGenerator.generate(), VerificationCardState.CONFIRMED, LocalDateTimeUtils.now()))
					.collect(toImmutableList());
			final String votingCardId = votingCards.get(0).votingCardId();

			when(verificationCardService.searchVotingCard(votingCardId)).thenReturn(
					new VotingCardSearchDto(
							votingCards.subList(0, LIMIT),
							new VotingCardSearchDto.Metadata(LIMIT, votingCards.size())
					)
			);

			final String jsonResult = webTestClient
					.get()
					.uri(UriComponentsBuilder.fromUriString(BASE_API + "/votingcards/?partialVotingCardId=" + votingCardId).toUriString())
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk()
					.returnResult(String.class)
					.getResponseBody()
					.blockFirst();

			final VotingCardSearchDto votingCardSearchDto = objectMapper.readValue(jsonResult, new TypeReference<>() {
			});

			assertEquals(votingCards.subList(0, LIMIT), votingCardSearchDto.votingCards());
		}

		@DisplayName("with a valid parameter but no matching result returns successfully")
		@Test
		void searchHappyPathEmptyResults() throws JsonProcessingException {
			final String votingCardId = uuidGenerator.generate();

			when(verificationCardService.searchVotingCard(votingCardId)).thenReturn(
					new VotingCardSearchDto(
							ImmutableList.of(),
							new VotingCardSearchDto.Metadata(LIMIT, 0)
					)
			);

			final String jsonResult = webTestClient
					.get()
					.uri(UriComponentsBuilder.fromUriString(BASE_API + "/votingcards/?partialVotingCardId=" + votingCardId).toUriString())
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk()
					.returnResult(String.class)
					.getResponseBody()
					.blockFirst();

			final VotingCardSearchDto votingCardSearchDto = objectMapper.readValue(jsonResult, new TypeReference<>() {
			});

			assertTrue(votingCardSearchDto.votingCards().isEmpty());
		}

		@DisplayName("with invalid parameter returns 412")
		@Test
		void searchWithInvalidParameterReturns412() {
			webTestClient
					.get()
					.uri(UriComponentsBuilder.fromUriString(BASE_API + "/votingcards/?partialVotingCardId=invalid").toUriString())
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isEqualTo(HttpStatus.PRECONDITION_FAILED);
		}
	}

	@DisplayName("get the used voting cards of an election event")
	@Nested
	class getUsedVotingCards {
		private final String targetUrl = BASE_API + "/electionevents/%s/votingcards/used";
		private final String electionEventId = uuidGenerator.generate();

		@BeforeEach
		void setUp() {
			when(electionEventService.exists(electionEventId)).thenReturn(true);
			when(tenantService.existTenant(TENANT_ID)).thenReturn(true);
		}

		@DisplayName("without parameters returns successfully")
		@Test
		void happyPathNoParam() throws JsonProcessingException {

			final ImmutableList<UsedVotingCardDto> usedVotingCards = ImmutableList.of(
					new UsedVotingCardDto(
							electionEventId,
							uuidGenerator.generate(),
							uuidGenerator.generate(),
							uuidGenerator.generate(),
							VerificationCardState.CONFIRMED,
							LocalDateTimeUtils.now())
			);
			when(verificationCardService.getUsedVotingCardsByElectionEventIdAndSinceUsageDateTime(electionEventId, null)).thenReturn(
					usedVotingCards);

			final String URL = String.format(targetUrl, electionEventId);

			final String retrievedJson = webTestClient
					.get()
					.uri(URL)
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk()
					.returnResult(String.class)
					.getResponseBody()
					.blockFirst();

			final ImmutableList<UsedVotingCardDto> retrievedUsedVotingCards = objectMapper.readValue(retrievedJson, new TypeReference<>() {
			});

			assertEquals(usedVotingCards, retrievedUsedVotingCards);
		}

		@DisplayName("with a valid parameter returns successfully")
		@Test
		void happyPathWithParam() throws JsonProcessingException {
			final LocalDateTime now = LocalDateTimeUtils.now();
			final ImmutableList<UsedVotingCardDto> usedVotingCards = ImmutableList.of(
					new UsedVotingCardDto(
							electionEventId,
							uuidGenerator.generate(),
							uuidGenerator.generate(),
							uuidGenerator.generate(),
							VerificationCardState.CONFIRMED,
							now)
			);
			when(verificationCardService.getUsedVotingCardsByElectionEventIdAndSinceUsageDateTime(electionEventId, now)).thenReturn(
					usedVotingCards);

			final String URL = String.format(targetUrl, electionEventId) + "?from=" + LocalDateTimeUtils.format(now);

			final String retrievedJson = webTestClient
					.get()
					.uri(URL)
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk()
					.returnResult(String.class)
					.getResponseBody()
					.blockFirst();

			final ImmutableList<UsedVotingCardDto> retrievedUsedVotingCards = objectMapper.readValue(retrievedJson, new TypeReference<>() {
			});

			assertEquals(usedVotingCards, retrievedUsedVotingCards);
		}

		@DisplayName("without parameters but with no matching result returns successfully")
		@Test
		void happyPathNoParamEmptyResults() throws JsonProcessingException {
			when(verificationCardService.getUsedVotingCardsByElectionEventIdAndSinceUsageDateTime(electionEventId, null))
					.thenReturn(ImmutableList.of());

			final String URL = String.format(targetUrl, electionEventId);

			final String retrievedJson = webTestClient
					.get()
					.uri(URL)
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk()
					.returnResult(String.class)
					.getResponseBody()
					.blockFirst();

			final ImmutableList<UsedVotingCardDto> retrievedUsedVotingCards = objectMapper.readValue(retrievedJson, new TypeReference<>() {
			});

			assertTrue(retrievedUsedVotingCards.isEmpty());
		}

		@DisplayName("with a non-existing election event id returns 404")
		@Test
		void electionEventNotfound() {
			when(electionEventService.exists(electionEventId)).thenReturn(false);

			final String URL = String.format(targetUrl, electionEventId);

			assertNotNull(webTestClient
					.get()
					.uri(URL)
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus()
					.isNotFound());
		}

		@DisplayName("with an invalid election event id returns 412")
		@Test
		void electionEventIdInvalid() {
			final String invalidElectionEventId = "invalidElectionEventId";

			final String URL = String.format(targetUrl, invalidElectionEventId);

			assertNotNull(webTestClient
					.get()
					.uri(URL)
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isEqualTo(HttpStatus.PRECONDITION_FAILED));
		}

		@DisplayName("with an invalid parameter returns 412")
		@Test
		void fromDateInvalid() {
			final String URL = String.format(targetUrl, electionEventId);

			assertNotNull(webTestClient
					.get()
					.uri(URL + "?from=invalid")
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isEqualTo(HttpStatus.PRECONDITION_FAILED));
		}
	}

	@DisplayName("get a voting card")
	@Nested
	class GetVotingCard {
		private final String targetUrl = BASE_API + "/votingcards/%s";
		private final String votingCardId = uuidGenerator.generate();

		@DisplayName("with a valid parameter returns successfully")
		@Test
		void happyPath() throws JsonProcessingException {
			final VotingCardDto votingCardDto = new VotingCardDto(
					uuidGenerator.generate(),
					uuidGenerator.generate(),
					uuidGenerator.generate(),
					votingCardId,
					VerificationCardState.CONFIRMED,
					LocalDateTimeUtils.now()
			);

			when(verificationCardService.getVotingCard(votingCardId)).thenReturn(votingCardDto);

			final String retrievedJson = webTestClient
					.get()
					.uri(String.format(targetUrl, votingCardId))
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk()
					.returnResult(String.class)
					.getResponseBody()
					.blockFirst();

			final VotingCardDto retrievedVotingCard = objectMapper.readValue(retrievedJson, VotingCardDto.class);

			assertEquals(votingCardDto, retrievedVotingCard);
		}

		@DisplayName("with a non-existing voting card id returns 404")
		@Test
		void votingCardNotFound() {
			when(verificationCardService.getVotingCard(votingCardId)).thenThrow(new VerificationCardNotFoundException("Not found"));

			assertNotNull(webTestClient
					.get()
					.uri(String.format(targetUrl, votingCardId))
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isNotFound());
		}

		@DisplayName("with an invalid voting card id returns 412")
		@Test
		void votingCardIdInvalid() {
			final String invalidVotingCardId = "invalidVotingCardId";

			assertNotNull(webTestClient
					.get()
					.uri(String.format(targetUrl, invalidVotingCardId))
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isEqualTo(HttpStatus.PRECONDITION_FAILED));
		}
	}

	@DisplayName("block a voting card")
	@Nested
	class BlockVotingCard {
		private final String targetUrl = BASE_API + "/votingcards/%s/block";
		private final String votingCardId = uuidGenerator.generate();

		@DisplayName("with a valid parameter returns successfully")
		@Test
		void happyPath() {

			doNothing().when(verificationCardService).blockVotingCard(votingCardId);

			webTestClient
					.put()
					.uri(String.format(targetUrl, votingCardId))
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk();
		}

		@DisplayName("with an invalid voting card id returns 412")
		@Test
		void votingCardIdInvalid() {
			final String invalidVotingCardId = "invalidVotingCardId";

			assertNotNull(webTestClient
					.put()
					.uri(String.format(targetUrl, invalidVotingCardId))
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isEqualTo(HttpStatus.PRECONDITION_FAILED));
		}

		@DisplayName("with a non-existing voting card id returns 404")
		@Test
		void votingCardNotFound() {
			doThrow(new VerificationCardNotFoundException("")).when(verificationCardService).blockVotingCard(votingCardId);

			assertNotNull(webTestClient
					.put()
					.uri(String.format(targetUrl, votingCardId))
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isNotFound());
		}

		@DisplayName("with a voting card whose status does not allow blocking returns 409")
		@Test
		void invalidState() {
			doThrow(new InvalidVerificationCardStateException(votingCardId, VerificationCardState.CONFIRMED))
					.when(verificationCardService).blockVotingCard(votingCardId);

			assertNotNull(webTestClient
					.put()
					.uri(String.format(targetUrl, votingCardId))
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isEqualTo(HttpStatus.CONFLICT));
		}
	}

	@DisplayName("get all election events")
	@Nested
	class GetAllElectionEvents {

		@DisplayName("returns successfully")
		@Test
		void happyPath() throws JsonProcessingException {
			final LocalDateTime startTime = LocalDateTimeUtils.now();
			final LocalDateTime finishTime = startTime.plusDays(1);

			final ImmutableList<ElectionEventDto> electionEvents = ImmutableList.of(
					new ElectionEventDto(uuidGenerator.generate(), "Alias1", "Description1", startTime, finishTime),
					new ElectionEventDto(uuidGenerator.generate(), "Alias2", "Description2", startTime, finishTime)
			);

			when(electionEventContextService.retrieveAll()).thenReturn(electionEvents);

			final String retrievedJson = webTestClient
					.get()
					.uri(BASE_API + "/electionevents/")
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk()
					.returnResult(String.class)
					.getResponseBody()
					.blockFirst();

			final ImmutableList<ElectionEventDto> retrievedElectionEvents = objectMapper.readValue(retrievedJson, new TypeReference<>() {
			});

			assertEquals(electionEvents, retrievedElectionEvents);
		}

		@DisplayName("without any known election events returns successfully")
		@Test
		void happyPathEmptyResults() throws JsonProcessingException {
			when(electionEventContextService.retrieveAll()).thenReturn(ImmutableList.of());

			final String retrievedJson = webTestClient
					.get()
					.uri(BASE_API + "/electionevents/")
					.header(HEADER_TENANT_ID, TENANT_ID)
					.exchange()
					.expectStatus().isOk()
					.returnResult(String.class)
					.getResponseBody()
					.blockFirst();

			final ImmutableList<ElectionEventDto> retrievedElectionEvents = objectMapper.readValue(retrievedJson, new TypeReference<>() {
			});

			assertTrue(retrievedElectionEvents.isEmpty());
		}
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
