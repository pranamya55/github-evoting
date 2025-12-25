/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.securedatamanager.online.process.download.DownloadEncryptedLongReturnCodeSharesService;
import ch.post.it.evoting.securedatamanager.online.process.download.DownloadVerificationCardSetService;
import ch.post.it.evoting.securedatamanager.setup.process.generate.EncryptedNodeLongReturnCodeSharesService;
import ch.post.it.evoting.securedatamanager.setup.process.generate.ReturnCodesPayloadsGenerateService;
import ch.post.it.evoting.securedatamanager.setup.process.preconfigure.ElectionEventContextPersistenceService;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentCodeSharesPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentCMTablePayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetStateRepository;

@Configuration
public class VerificationCardSetServiceTestSpringConfig {

	@Bean
	public PathResolver pathResolver() {
		return mock(PathResolver.class);
	}

	@Bean
	public VerificationCardSetService verificationCardSetService() {
		return mock(VerificationCardSetService.class);
	}

	@Bean
	public VerificationCardSetStateRepository verificationCardSetStateRepository() {
		return mock(VerificationCardSetStateRepository.class);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return DomainObjectMapper.getNewInstance();
	}

	@Bean
	public DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService() {
		return mock(DownloadEncryptedLongReturnCodeSharesService.class);
	}

	@Bean
	public SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileSystemRepository() {
		return mock(SetupComponentVerificationDataPayloadFileRepository.class);
	}

	@Bean
	public EncryptedNodeLongReturnCodeSharesService encryptedNodeLongCodeSharesService() {
		return mock(EncryptedNodeLongReturnCodeSharesService.class);
	}

	@Bean
	public ControlComponentCodeSharesPayloadFileRepository nodeContributionsResponsesFileRepository(final ObjectMapper objectMapper,
			final PathResolver pathResolver) {
		return new ControlComponentCodeSharesPayloadFileRepository(objectMapper, pathResolver);
	}

	@Bean
	public DownloadVerificationCardSetService downloadVerificationCardSetService(
			final VerificationCardSetService verificationCardSetService,
			final DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService,
			final ControlComponentCodeSharesPayloadFileRepository controlComponentCodeSharesPayloadFileRepository,
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository) {
		return new DownloadVerificationCardSetService(true, verificationCardSetService, downloadEncryptedLongReturnCodeSharesService,
				controlComponentCodeSharesPayloadFileRepository, setupComponentVerificationDataPayloadFileRepository);
	}

	@Bean
	public BallotBoxService ballotBoxService() {
		return mock(BallotBoxService.class);
	}

	@Bean
	public ElectionEventContextPersistenceService electionEventContextPersistenceService() {
		return mock(ElectionEventContextPersistenceService.class);
	}

	@Bean
	public SetupComponentCMTablePayloadService returnCodesMappingTablePayloadService() {
		return mock(SetupComponentCMTablePayloadService.class);
	}

	@Bean
	public ReturnCodesPayloadsGenerateService returnCodesMappingTablePayloadGenerationService() {
		return mock(ReturnCodesPayloadsGenerateService.class);
	}

}
