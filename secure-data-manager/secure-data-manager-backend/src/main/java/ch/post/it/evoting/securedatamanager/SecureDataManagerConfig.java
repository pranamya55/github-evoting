/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base16;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetFactory;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricFactory;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.direct.trust.SignatureKeystoreFactory;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.StreamableSymmetricEncryptionDecryptionService;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyKeyGenerationSchnorrProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.FactorizeAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.DecodeWriteInsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.IntegerToWriteInAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.IsWriteInOptionAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.QuadraticResidueToWriteInAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.ProcessPlaintextsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyMixDecOfflineAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyVotingClientProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;
import ch.post.it.evoting.securedatamanager.shared.KeystoreRepository;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.workflow.ServerMode;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowLogService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowService;

import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Configuration
public class SecureDataManagerConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(SecureDataManagerConfig.class);

	@Bean
	public ObjectMapper objectMapper() {
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

		objectMapper.tokenStreamFactory().setStreamReadConstraints(StreamReadConstraints
				.builder()
				.maxStringLength(Integer.MAX_VALUE)
				.build());

		return objectMapper;
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ObjectReader readerForDeserialization() {
		final ObjectMapper mapper = mapperForDeserialization();
		return mapper.reader();
	}

	private ObjectMapper mapperForDeserialization() {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
		mapper.findAndRegisterModules();
		return mapper;
	}

	@Bean
	public Random random() {
		return RandomFactory.createRandom();
	}

	@Bean
	public Mixnet mixnet() {
		return MixnetFactory.createMixnet();
	}

	@Bean
	public ZeroKnowledgeProof zeroKnowledgeProof() {
		return ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
	}

	@Bean
	public Hash cryptoPrimitivesHash() {
		return HashFactory.createHash();
	}

	@Bean
	ElGamal elGamal() {
		return ElGamalFactory.createElGamal();
	}

	@Bean
	Base64 base64() {
		return BaseEncodingFactory.createBase64();
	}

	@Bean
	Base16 base16() {
		return BaseEncodingFactory.createBase16();
	}

	@Bean
	KeyDerivation keyDerivation() {
		return KeyDerivationFactory.createKeyDerivation();
	}

	@Bean
	Symmetric symmetric() {
		return SymmetricFactory.createSymmetric();
	}

	@Bean
	GetHashContextAlgorithm getHashContextAlgorithm(final Base64 base64, final Hash hash,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		return new GetHashContextAlgorithm(base64, hash, primesMappingTableAlgorithms);
	}

	@Bean
	GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm(final Base64 base64, final Hash hash) {
		return new GetHashElectionEventContextAlgorithm(base64, hash);
	}

	@Bean
	PrimesMappingTableAlgorithms primesMappingTableAlgorithms() {
		return new PrimesMappingTableAlgorithms();
	}

	@Bean
	FactorizeAlgorithm factorizeAlgorithm() {
		return new FactorizeAlgorithm();
	}

	@Bean
	IsWriteInOptionAlgorithm isWriteInOptionAlgorithm() {
		return new IsWriteInOptionAlgorithm();
	}

	@Bean
	IntegerToWriteInAlgorithm integerToWriteInAlgorithm() {
		return new IntegerToWriteInAlgorithm();
	}

	@Bean
	QuadraticResidueToWriteInAlgorithm quadraticResidueToWriteInAlgorithm(final IntegerToWriteInAlgorithm integerToWriteInAlgorithm) {
		return new QuadraticResidueToWriteInAlgorithm(integerToWriteInAlgorithm);
	}

	@Bean
	DecodeWriteInsAlgorithm decodeWriteInsAlgorithm(final IsWriteInOptionAlgorithm isWriteInOptionAlgorithm,
			final QuadraticResidueToWriteInAlgorithm quadraticResidueToWriteInAlgorithm) {
		return new DecodeWriteInsAlgorithm(isWriteInOptionAlgorithm, quadraticResidueToWriteInAlgorithm);
	}

	@Bean
	GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm(final Hash hash, final Base64 base64, final ElGamal elGamal) {
		return new GetMixnetInitialCiphertextsAlgorithm(hash, base64, elGamal);
	}

	@Bean
	ProcessPlaintextsAlgorithm processPlaintextsAlgorithm(final ElGamal elGamal,
			final FactorizeAlgorithm factorizeAlgorithm,
			final DecodeWriteInsAlgorithm decodeWriteInsAlgorithm,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		return new ProcessPlaintextsAlgorithm(elGamal, factorizeAlgorithm, decodeWriteInsAlgorithm, primesMappingTableAlgorithms);
	}

	@Bean
	VerifyMixDecOfflineAlgorithm verifyMixDecOfflineAlgorithm(final ElGamal elGamal,
			final Mixnet mixnet,
			final ZeroKnowledgeProof zeroKnowledgeProof) {
		return new VerifyMixDecOfflineAlgorithm(elGamal, mixnet, zeroKnowledgeProof);
	}

	@Bean
	VerifyVotingClientProofsAlgorithm verifyVotingClientProofsAlgorithm(final ZeroKnowledgeProof zeroKnowledgeProof,
			final GetHashContextAlgorithm getHashContextAlgorithm, final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		return new VerifyVotingClientProofsAlgorithm(zeroKnowledgeProof, getHashContextAlgorithm, primesMappingTableAlgorithms);
	}

	@Bean
	VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm(final ZeroKnowledgeProof zeroKnowledgeProof) {
		return new VerifyCCSchnorrProofsAlgorithm(zeroKnowledgeProof);
	}

	@Bean
	VerifyKeyGenerationSchnorrProofsAlgorithm verifyKeyGenerationSchnorrProofsAlgorithm(
			final VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm,
			final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm) {
		return new VerifyKeyGenerationSchnorrProofsAlgorithm(verifyCCSchnorrProofsAlgorithm, getHashElectionEventContextAlgorithm);
	}

	@Bean
	ExecutorService fixedThreadExecutorService(
			@Value("${fixed-thread-pool.available-processors-usage-rate}")
			final double availableProcessorsUsageRate) {
		checkArgument(0 < availableProcessorsUsageRate && availableProcessorsUsageRate <= 1,
				"Property 'fixed-thread-pool.available-processors-usage-rate' must be in the range (0, 1]. [Current: %s]",
				availableProcessorsUsageRate);

		final int availableProcessors = Runtime.getRuntime().availableProcessors();
		final int numberOfThreads = Integer.max(1, (int) (availableProcessors * availableProcessorsUsageRate));

		final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

		LOGGER.debug("Created fixed thread pool executor with {} threads.", numberOfThreads);
		return executorService;
	}

	@Bean
	ExecutorService singleThreadExecutorService() {
		return Executors.newSingleThreadExecutor();
	}

	@Bean
	XMLSignatureService xmlSignatureService() {
		return new XMLSignatureService();
	}

	@Bean
	@ConditionalOnProperty("role.isSetup")
	KeystoreRepository keystoreConfigRepository(
			@Value("${direct-trust.keystore.location:}")
			final Path keystoreLocation,
			@Value("${direct-trust.password.location:}")
			final Path keystorePasswordLocation,
			@Value("${direct-trust.keystore.filename-pattern}")
			final String directTrustKeystoreFilenamePattern,
			@Value("${direct-trust.password.filename-pattern}")
			final String directTrustPasswordFilenamePattern) throws IOException {
		return new KeystoreRepository(keystoreLocation, keystorePasswordLocation, Alias.SDM_CONFIG, directTrustKeystoreFilenamePattern,
				directTrustPasswordFilenamePattern);
	}

	@Bean
	@ConditionalOnProperty("role.isTally")
	KeystoreRepository keystoreTallyRepository(
			@Value("${direct-trust.keystore.location:}")
			final Path keystoreLocation,
			@Value("${direct-trust.password.location:}")
			final Path keystorePasswordLocation,
			@Value("${direct-trust.keystore.filename-pattern}")
			final String directTrustKeystoreFilenamePattern,
			@Value("${direct-trust.password.filename-pattern}")
			final String directTrustPasswordFilenamePattern) throws IOException {
		return new KeystoreRepository(keystoreLocation, keystorePasswordLocation, Alias.SDM_TALLY, directTrustKeystoreFilenamePattern,
				directTrustPasswordFilenamePattern);
	}

	@Bean
	@Conditional(RoleCondition.class)
	SignatureKeystore<Alias> signatureKeystoreService(
			@Value("${role.isSetup}")
			final boolean isSetup,
			@Value("${role.isTally}")
			final boolean isTally,
			final KeystoreRepository repository) throws IOException {

		LOGGER.debug("Creating a signature keystore service... [role.isSetup: {}, role.isTally: {}]", isSetup, isTally);

		final SignatureKeystore<Alias> signatureKeystore = SignatureKeystoreFactory.createSignatureKeystore(repository.getKeyStore(),
				repository.getKeystorePassword(), repository.getKeystoreAlias());

		LOGGER.info("Created a signature keystore service. [role.isSetup: {}, role.isTally: {}]", isSetup, isTally);

		return signatureKeystore;
	}

	@Bean
	@ConditionalOnProperty("role.isSetup")
	public WorkflowService setupWorkflowService(
			final BallotBoxService ballotBoxService,
			final WorkflowLogService workflowLogService,
			final ElectionEventService electionEventService) {
		return new WorkflowService(ballotBoxService, workflowLogService, electionEventService, ServerMode.SERVER_MODE_SETUP);
	}

	@Bean
	@ConditionalOnProperty("role.isTally")
	public WorkflowService tallyWorkflowService(
			final BallotBoxService ballotBoxService,
			final WorkflowLogService workflowLogService,
			final ElectionEventService electionEventService) {
		return new WorkflowService(ballotBoxService, workflowLogService, electionEventService, ServerMode.SERVER_MODE_TALLY);
	}

	@Bean
	@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
	public WorkflowService onlineWorkflowService(
			final BallotBoxService ballotBoxService,
			final WorkflowLogService workflowLogService,
			final ElectionEventService electionEventService) {
		return new WorkflowService(ballotBoxService, workflowLogService, electionEventService, ServerMode.SERVER_MODE_ONLINE);
	}

	@Bean
	@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
	public RetryBackoffSpec retryBackoffSpec(
			@Value("${spring.webflux.retry.backoff.max-attempts}")
			final int maxAttempts,
			@Value("${spring.webflux.retry.backoff.min-backoff}")
			final int minBackoff) {
		return Retry.backoff(maxAttempts, Duration.ofMillis(minBackoff));
	}

	@Bean
	Argon2 argon2Standard() {
		return Argon2Factory.createArgon2(Argon2Profile.STANDARD);
	}

	@Bean
	Argon2 argon2LessMemory() {
		return Argon2Factory.createArgon2(Argon2Profile.LESS_MEMORY);
	}

	@Bean
	StreamableSymmetricEncryptionDecryptionService streamableSymmetricEncryptionDecryptionService(
			final Random random,
			@Qualifier("argon2Standard")
			final Argon2 argon2) {
		return new StreamableSymmetricEncryptionDecryptionService(random, argon2);
	}

	static class RoleCondition extends AnyNestedCondition {

		public RoleCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(name = "role.isSetup")
		static class IsSetupCondition {
		}

		@ConditionalOnProperty(name = "role.isTally")
		static class IsTallyCondition {
		}

	}
}
