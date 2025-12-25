/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.collectdataverifier;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.xml.XsdConstants.TALLY_COMPONENT_ECH_0222_VERSION;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_ELECTION_EVENT_CONTEXT_PAYLOAD;
import static ch.post.it.evoting.securedatamanager.shared.Constants.TALLY_COMPONENT_ECH_0222_XML;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ech.xmlns.ech_0155._5.ExtensionType;
import ch.ech.xmlns.ech_0222._3.Delivery;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.KeystoreRepository;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBox;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.tally.process.TallyComponentVotesFileRepository;
import ch.post.it.evoting.securedatamanager.tally.process.TallyComponentVotesService;
import ch.post.it.evoting.securedatamanager.tally.process.TallyPathResolver;

@ExtendWith(MockitoExtension.class)
class TallyComponentFilesServiceTest {

	private static final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

	private static final TallyPathResolver pathResolverTempMock = mock(TallyPathResolver.class);
	private static final TallyPathResolver pathResolverMock = mock(TallyPathResolver.class);
	private static final BallotBoxService ballotBoxServiceMock = mock(BallotBoxService.class);
	private static final ElectionEventService electionEventServiceMock = mock(ElectionEventService.class);

	private static Path testFolderPath;
	private static KeystoreRepository keystoreRepository;
	private static XMLSignatureService xmlSignatureService;
	private static TallyComponentEch0222Service tallyComponentEch0222Service;
	private static TallyComponentFilesService tallyComponentFilesService;
	private static String electionEventId;
	private static String seed;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws URISyntaxException, IOException {

		testFolderPath = Paths.get(Objects.requireNonNull(TallyComponentFilesServiceTest.class.getResource("/")).toURI());

		final ElectionEventContextPayload electionEventContextPayload = objectMapper.readValue(
				testFolderPath.resolve(CONFIG_FILE_NAME_ELECTION_EVENT_CONTEXT_PAYLOAD).toFile(), ElectionEventContextPayload.class);
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		seed = electionEventContextPayload.getSeed();
		final String filename = String.format(TALLY_COMPONENT_ECH_0222_XML, TALLY_COMPONENT_ECH_0222_VERSION, validateSeed(seed));

		when(pathResolverTempMock.resolveConfigurationPath()).thenReturn(testFolderPath);
		when(pathResolverTempMock.resolveElectionEventPath(electionEventId)).thenReturn(tempDir);
		when(pathResolverTempMock.resolveTallyOutputPath()).thenReturn(tempDir);
		when(pathResolverTempMock.resolveTallyOutputPath().resolve(filename)).thenReturn(tempDir);

		when(pathResolverMock.resolveConfigurationPath()).thenReturn(testFolderPath);
		when(pathResolverMock.resolveElectionEventPath(electionEventId)).thenReturn(testFolderPath);
		when(pathResolverMock.resolveTallyOutputPath()).thenReturn(testFolderPath);
		when(pathResolverMock.resolveTallyOutputPath().resolve(filename)).thenReturn(testFolderPath);

		final ImmutableList<BallotBox> ballotBoxes = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().stream()
				.map(verificationCardSetContext -> {
					final String ballotBoxId = verificationCardSetContext.getBallotBoxId();

					when(pathResolverTempMock.resolveBallotBoxPath(electionEventId, ballotBoxId))
							.thenReturn(testFolderPath.resolve(Constants.BALLOT_BOXES).resolve(ballotBoxId));

					return new BallotBox(ballotBoxId, verificationCardSetContext.getVerificationCardSetDescription(),
							verificationCardSetContext.getBallotBoxStartTime(), verificationCardSetContext.getBallotBoxFinishTime(),
							verificationCardSetContext.isTestBallotBox(), verificationCardSetContext.getGracePeriod(), "DECRYPTED");
				})
				.collect(toImmutableList());

		xmlSignatureService = new XMLSignatureService();
		final Path keyStorePasswordPath = testFolderPath.resolve("local_direct_trust_pw_sdm_tally.txt");
		final Path keyStorePath = testFolderPath.resolve("local_direct_trust_keystore_sdm_tally.p12");
		keystoreRepository = new KeystoreRepository(keyStorePath, keyStorePasswordPath, Alias.SDM_TALLY,
				"direct_trust_keystore_sdm_", "direct_trust_pw_sdm_");

		when(electionEventServiceMock.findElectionEventId()).thenReturn(electionEventId);
		final EvotingConfigService evotingConfigService = new EvotingConfigService(electionEventServiceMock,
				new TallyEvotingConfigFileRepository(pathResolverTempMock, electionEventServiceMock, keystoreRepository, xmlSignatureService));

		tallyComponentEch0222Service = new TallyComponentEch0222Service(
				evotingConfigService,
				new TallyComponentEch0222FileRepository(pathResolverTempMock, keystoreRepository, xmlSignatureService, seed));

		final TallyComponentVotesService tallyComponentVotesService = new TallyComponentVotesService(
				new TallyComponentVotesFileRepository(pathResolverTempMock, DomainObjectMapper.getNewInstance()));

		when(ballotBoxServiceMock.getBallotBoxes(electionEventId)).thenReturn(ballotBoxes);

		tallyComponentFilesService = new TallyComponentFilesService(ballotBoxServiceMock, evotingConfigService, tallyComponentVotesService,
				tallyComponentEch0222Service);
	}

	@Test
	void happyPath() {
		final Delivery original = new TallyComponentEch0222Service(
				new EvotingConfigService(electionEventServiceMock,
						new TallyEvotingConfigFileRepository(pathResolverMock, electionEventServiceMock, keystoreRepository, xmlSignatureService)),
				new TallyComponentEch0222FileRepository(pathResolverMock, keystoreRepository, xmlSignatureService, seed))
				.load(electionEventId);

		assertDoesNotThrow(() -> tallyComponentFilesService.generate(electionEventId));
		final Delivery regenerated = tallyComponentEch0222Service.load(electionEventId);

		assertSameEch0222(original, regenerated);
	}

	private void assertSameEch0222(final Delivery original, final Delivery regenerated) {

		final ExtensionType originalExtension = original.getRawDataDelivery().getExtension();
		final ExtensionType regeneratedExtension = regenerated.getRawDataDelivery().getExtension();

		final Element originalSignature = (Element) originalExtension.getAny().getFirst();
		final Element regeneratedSignature = (Element) regeneratedExtension.getAny().getFirst();

		final String originalDigestValue = originalSignature.getFirstChild().getLastChild().getLastChild().getTextContent();
		final String regeneratedDigestValue = regeneratedSignature.getFirstChild().getLastChild().getLastChild().getTextContent();

		assertEquals(originalDigestValue, regeneratedDigestValue);
	}
}
