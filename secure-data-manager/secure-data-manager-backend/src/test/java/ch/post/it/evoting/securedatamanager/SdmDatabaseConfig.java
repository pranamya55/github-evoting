/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager;

import static org.mockito.Mockito.mock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;

import ch.post.it.evoting.securedatamanager.online.process.download.DownloadEncryptedLongReturnCodeSharesService;
import ch.post.it.evoting.securedatamanager.online.process.download.DownloadVerificationCardSetService;
import ch.post.it.evoting.securedatamanager.setup.process.generate.EncryptedNodeLongReturnCodeSharesService;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentCodeSharesPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

/**
 * MVC Configuration
 */
@Configuration
@ComponentScan(basePackages = { "ch.post.it.evoting.securedatamanager.shared" })
@PropertySource("classpath:config/application.properties")
@Profile("test")
public class SdmDatabaseConfig {

	@Value("${sdm.path.workspace}")
	private String workspace;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public DownloadVerificationCardSetService verificationCardSetDownloadService(
			final VerificationCardSetService verificationCardSetService,
			final DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService,
			final ControlComponentCodeSharesPayloadFileRepository controlComponentCodeSharesPayloadFileRepository,
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository) {
		return new DownloadVerificationCardSetService(false, verificationCardSetService, downloadEncryptedLongReturnCodeSharesService,
				controlComponentCodeSharesPayloadFileRepository, setupComponentVerificationDataPayloadFileRepository);
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return mock(RestTemplate.class);
	}

	@Bean
	EncryptedNodeLongReturnCodeSharesService encryptedNodeLongCodeSharesService() {
		return mock(EncryptedNodeLongReturnCodeSharesService.class);
	}

	@Bean
	DownloadEncryptedLongReturnCodeSharesService encryptedLongReturnCodeSharesDownloadService() {
		return mock(DownloadEncryptedLongReturnCodeSharesService.class);
	}
}
