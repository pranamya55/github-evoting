/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.domain.generators.ControlComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally.SetupTallyEBAlgorithm;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally.SetupTallyEBService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenVerCardSetKeysAlgorithm;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenVerCardSetKeysService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardHashesPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardHashesPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentPublicKeysPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentPublicKeysPayloadService;

@DisplayName("An ElectoralBoardConstitutionService")
@ExtendWith(MockitoExtension.class)
class ElectoralBoardConstitutionServiceTest {

	private static final ImmutableList<SafePasswordHolder> PASSWORDS = ImmutableList.of(
			new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()),
			new SafePasswordHolder("Password_ElectoralBoard2".toCharArray()));
	private static final ImmutableList<ImmutableByteArray> HASHES = PASSWORDS.stream()
			.map(SafePasswordHolder::get)
			.map(CharBuffer::wrap)
			.map(StandardCharsets.UTF_8::encode)
			.map(ByteBuffer::array)
			.map(ImmutableByteArray::new)
			.collect(toImmutableList());

	private static String electionEventId;
	private static PathResolver pathResolver;
	private static ElectoralBoardConstitutionService electoralBoardConstitutionService;

	@BeforeAll
	static void setUp(
			@TempDir
			final Path tempDir) throws IOException, SignatureException {
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

		final ControlComponentPublicKeysPayloadGenerator controlComponentPublicKeysPayloadGenerator = new ControlComponentPublicKeysPayloadGenerator();
		final int maximumNumberOfWriteInsPlusOne = 16;
		final ImmutableList<ControlComponentPublicKeysPayload> controlComponentPublicKeysPayloads = controlComponentPublicKeysPayloadGenerator.generate(
				maximumNumberOfWriteInsPlusOne);
		electionEventId = controlComponentPublicKeysPayloads.get(0).getElectionEventId();

		final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeysList = controlComponentPublicKeysPayloads.stream()
				.map(ControlComponentPublicKeysPayload::getControlComponentPublicKeys)
				.collect(toImmutableList());

		final ControlComponentPublicKeysConfigService controlComponentPublicKeysConfigServiceMock = mock(
				ControlComponentPublicKeysConfigService.class);
		when(controlComponentPublicKeysConfigServiceMock.loadOrderByNodeId(electionEventId)).thenReturn(controlComponentPublicKeysList);

		final ElectionEventService electionEventServiceMock = mock(ElectionEventService.class);
		when(electionEventServiceMock.exists(electionEventId)).thenReturn(true);

		final ZeroKnowledgeProof zeroKnowledgeProofServiceMock = mock(ZeroKnowledgeProof.class);
		final ZqGroup zqGroup = controlComponentPublicKeysList.get(0).ccmjSchnorrProofs().getGroup();
		when(zeroKnowledgeProofServiceMock.genSchnorrProof(any(), any(), any())).thenReturn(
				new SchnorrProof(ZqElement.create(3, zqGroup), ZqElement.create(2, zqGroup)));
		when(zeroKnowledgeProofServiceMock.verifySchnorrProof(any(), any(), any())).thenReturn(true);

		pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final SetupComponentPublicKeysPayloadFileRepository setupComponentPublicKeysPayloadFileRepository = new SetupComponentPublicKeysPayloadFileRepository(
				objectMapper, pathResolver);

		final ElectoralBoardHashesPayloadFileRepository electoralBoardHashesPayloadFileRepository = new ElectoralBoardHashesPayloadFileRepository(
				objectMapper, pathResolver);

		final ElectionEventContextPayloadService electionEventContextPayloadService = mock(ElectionEventContextPayloadService.class);
		final GqGroup encryptionGroup = controlComponentPublicKeysList.get(0).ccmjElectionPublicKey().getGroup();
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator(encryptionGroup);
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate(
				controlComponentPublicKeysList.get(0).ccrjChoiceReturnCodesEncryptionPublicKey().size(),
				controlComponentPublicKeysList.get(0).ccmjElectionPublicKey().size());

		final SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService = new SetupComponentPublicKeysPayloadService(
				setupComponentPublicKeysPayloadFileRepository);

		final ElectoralBoardHashesPayloadService electoralBoardHashesPayloadService = new ElectoralBoardHashesPayloadService(
				electoralBoardHashesPayloadFileRepository);

		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator(encryptionGroup);
		final PrimesMappingTable primesMappingTable = primesMappingTableGenerator.generate(1);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String verificationCardSetId = uuidGenerator.generate();
		when(electionEventContextPayloadService.loadPrimesMappingTable(electionEventId, verificationCardSetId)).thenReturn(primesMappingTable);

		final SignatureKeystore<Alias> signatureKeystoreServiceSdmConfigEBMock = mock(SignatureKeystore.class);
		final ElectoralBoardPersistenceService electoralBoardPersistenceService = new ElectoralBoardPersistenceService(
				setupComponentPublicKeysPayloadService, signatureKeystoreServiceSdmConfigEBMock, electoralBoardHashesPayloadService);
		when(signatureKeystoreServiceSdmConfigEBMock.generateSignature(any(), any())).thenReturn(ImmutableByteArray.of((byte) 1, (byte) 2, (byte) 3));

		when(electionEventContextPayloadService.load(electionEventId)).thenReturn(electionEventContextPayload);

		final Hash hash = HashFactory.createHash();
		final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm = new GetHashElectionEventContextAlgorithm(
				BaseEncodingFactory.createBase64(), hash);
		final SetupTallyEBAlgorithm setupTallyEBAlgorithm = new SetupTallyEBAlgorithm(hash, ElGamalFactory.createElGamal(),
				zeroKnowledgeProofServiceMock,
				new VerifyCCSchnorrProofsAlgorithm(zeroKnowledgeProofServiceMock), getHashElectionEventContextAlgorithm);
		final SetupTallyEBService setupTallyEBService = new SetupTallyEBService(setupTallyEBAlgorithm
		);

		final GenVerCardSetKeysAlgorithm genVerCardSetKeysAlgorithm = new GenVerCardSetKeysAlgorithm(ElGamalFactory.createElGamal(),
				new VerifyCCSchnorrProofsAlgorithm(zeroKnowledgeProofServiceMock));
		final GenVerCardSetKeysService genVerCardSetKeysService = new GenVerCardSetKeysService(genVerCardSetKeysAlgorithm);
		electoralBoardConstitutionService = new ElectoralBoardConstitutionService(setupTallyEBService, electionEventServiceMock,
				genVerCardSetKeysService, electoralBoardPersistenceService, electionEventContextPayloadService,
				controlComponentPublicKeysConfigServiceMock,
				mock(SetupComponentVerificationCardKeystoresPayloadGenerationService.class)
		);
	}

	@Test
	@DisplayName("constitutes successfully and persists the payloads.")
	void constituteHappyPath() {
		assertDoesNotThrow(() -> electoralBoardConstitutionService.constitute(electionEventId, PASSWORDS, HASHES));
		assertTrue(Files.exists(pathResolver.resolveElectionEventPath(electionEventId).resolve("setupComponentElectoralBoardHashesPayload.json")));
	}

	@Test
	@DisplayName("provided with non-valid election event id or null arguments throws.")
	void nonValidArguments() {
		assertAll(
				() -> assertThrows(NullPointerException.class, () -> electoralBoardConstitutionService.constitute(null, PASSWORDS, HASHES)),
				() -> assertThrows(FailedValidationException.class, () -> electoralBoardConstitutionService.constitute("invalid", PASSWORDS, HASHES)),
				() -> assertThrows(NullPointerException.class,
						() -> electoralBoardConstitutionService.constitute(electionEventId, null, HASHES)),
				() -> assertThrows(NullPointerException.class,
						() -> electoralBoardConstitutionService.constitute(electionEventId, PASSWORDS, null))
		);
	}

}
