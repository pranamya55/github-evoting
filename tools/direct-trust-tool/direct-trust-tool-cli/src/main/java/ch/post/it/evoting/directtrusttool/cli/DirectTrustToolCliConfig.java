/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli;

import java.security.cert.CertificateException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.directtrusttool.backend.process.NameService;
import ch.post.it.evoting.directtrusttool.backend.process.PemConverterService;
import ch.post.it.evoting.directtrusttool.backend.process.downloadkeystores.KeystoresDownloadService;
import ch.post.it.evoting.directtrusttool.backend.process.generatekeystores.KeystoresGenerationService;
import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.PublicKeysSharingService;
import ch.post.it.evoting.directtrusttool.backend.session.FileRepository;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService;

@Configuration
public class DirectTrustToolCliConfig {

	@Bean
	public FileRepository fileRepository(
			@Value("${app.directory.output}")
			final String outputDirectory) {
		return new FileRepository(outputDirectory);
	}

	@Bean
	public SessionService sessionService(final FileRepository fileRepository, final ObjectMapper mapper) {
		return new SessionService(fileRepository, mapper);
	}

	@Bean
	public PemConverterService pemConverterService() throws CertificateException {
		return new PemConverterService();
	}

	@Bean
	public NameService nameService(final SessionService sessionService) {
		return new NameService(sessionService);
	}

	@Bean
	public KeystoresDownloadService keystoreDownloadService(
			final SessionService sessionService,
			final NameService nameService) {
		return new KeystoresDownloadService(sessionService, nameService);
	}

	@Bean
	public PublicKeysSharingService publicKeysService(
			final PemConverterService pemConverterService,
			final SessionService sessionService,
			final NameService nameService) {
		return new PublicKeysSharingService(sessionService, pemConverterService, nameService);
	}

	@Bean
	public KeystoresGenerationService keystoreGenerationService(
			final PemConverterService pemConverterService,
			final SessionService sessionService) {
		return new KeystoresGenerationService(sessionService, pemConverterService);
	}
}
