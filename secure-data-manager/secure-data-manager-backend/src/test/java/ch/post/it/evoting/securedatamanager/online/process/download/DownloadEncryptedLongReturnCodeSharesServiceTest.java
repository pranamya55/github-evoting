/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.download;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static java.nio.file.Files.exists;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.generators.ControlComponentCodeSharesPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.VerificationCardSetServiceTestSpringConfig;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(VerificationCardSetServiceTestSpringConfig.class)
class DownloadEncryptedLongReturnCodeSharesServiceTest {

	private static final String ELECTION_EVENT_ID = "A3D790FD1AC543F9B0A05CA79A20C9E2";
	private static final String VERIFICATION_CARD_SET_ID = "74A4E530B24F4086B099D153321CF1B3";
	private static final String PRECOMPUTED_VALUES_PATH = "computeTest";
	private static final String VERIFICATION_CARD_SETS_PATH = "verificationCardSets";

	@Autowired
	private DownloadVerificationCardSetService downloadVerificationCardSetService;
	@Autowired
	private PathResolver pathResolver;
	@Autowired
	private DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService;
	@Autowired
	private SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	@Test
	void download() throws URISyntaxException {
		final Path basePath = Paths.get(
				Objects.requireNonNull(DownloadEncryptedLongReturnCodeSharesServiceTest.class.getClassLoader().getResource(PRECOMPUTED_VALUES_PATH))
						.toURI());
		final Path verificationCardSetPath = basePath.resolve(ELECTION_EVENT_ID).resolve(VERIFICATION_CARD_SETS_PATH)
				.resolve(VERIFICATION_CARD_SET_ID);

		final ControlComponentCodeSharesPayloadGenerator controlComponentCodeSharesPayloadGenerator = new ControlComponentCodeSharesPayloadGenerator();
		final ImmutableList<ImmutableList<ControlComponentCodeSharesPayload>> payloads = IntStream.range(0, 3).mapToObj(
				i -> controlComponentCodeSharesPayloadGenerator.generate(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, i, 5, 2)
		).collect(toImmutableList());

		when(pathResolver.resolveVerificationCardSetPath(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)).thenReturn(verificationCardSetPath);
		when(setupComponentVerificationDataPayloadFileRepository.getCount(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)).thenReturn(3);
		when(downloadEncryptedLongReturnCodeSharesService.download(anyString(), anyString(), anyInt())).thenReturn(
				Flux.fromIterable(payloads).parallel());

		assertAll(() -> assertDoesNotThrow(
						() -> downloadVerificationCardSetService.download(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)),
				() -> assertTrue(
						exists(verificationCardSetPath.resolve(Constants.CONFIG_FILE_CONTROL_COMPONENT_CODE_SHARES_PAYLOAD + ".0" + Constants.JSON))),
				() -> assertTrue(
						exists(verificationCardSetPath.resolve(Constants.CONFIG_FILE_CONTROL_COMPONENT_CODE_SHARES_PAYLOAD + ".1" + Constants.JSON))),
				() -> assertTrue(exists(verificationCardSetPath.resolve(
						Constants.CONFIG_FILE_CONTROL_COMPONENT_CODE_SHARES_PAYLOAD + ".2" + Constants.JSON))));

	}
}
