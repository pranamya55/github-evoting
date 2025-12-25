/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.PublicKeysSharingService;
import ch.post.it.evoting.directtrusttool.cli.DirectTrustToolCliApplication;
import ch.post.it.evoting.directtrusttool.cli.FileService;

import picocli.CommandLine;

@SpringBootTest(webEnvironment = NONE, classes = DirectTrustToolCliApplication.class)
class PublicKeysSharingDownloadCommandTest {

	@MockitoBean
	PublicKeysSharingService publicKeysSharingService;
	@MockitoBean
	FileService fileService;

	@Autowired
	CommandLine.IFactory factory;

	@Autowired
	PublicKeysSharingDownloadCommand publicKeysSharingDownloadCommand;

	@AfterEach
	void tearDown() {
		Mockito.reset(publicKeysSharingService);
		Mockito.reset(fileService);
	}

	@Test
	void testDownloadPublicKeysWithDefaultSession() {
		// given
		final ImmutableByteArray expectedZip = ImmutableByteArray.EMPTY;
		final String expectedSessionId = "00000000000000000000000000000000";
		final Path expectedPath = Path.of("test.zip");

		given(publicKeysSharingService.downloadPublicKeys(expectedSessionId)).willReturn(expectedZip);

		// when
		final int exitCode = new CommandLine(publicKeysSharingDownloadCommand, factory)
				.execute("--output", "test.zip");

		// then
		assertEquals(0, exitCode);
		then(fileService).should().saveByteArrayAsZip(expectedZip, expectedPath);
		then(publicKeysSharingService).should().downloadPublicKeys(expectedSessionId);
	}

	@Test
	void testDownloadPublicKeysWithCustomSession() {
		// given
		final ImmutableByteArray expectedZip = ImmutableByteArray.EMPTY;
		final String expectedSessionId = "11111111111111111111111111111111";
		final Path expectedPath = Path.of("test.zip");

		given(publicKeysSharingService.downloadPublicKeys(expectedSessionId)).willReturn(expectedZip);

		// when
		final int exitCode = new CommandLine(publicKeysSharingDownloadCommand, factory)
				.execute(
						"--output", "test.zip",
						"--session-id", expectedSessionId
				);

		// then
		assertEquals(0, exitCode);
		then(fileService).should().saveByteArrayAsZip(expectedZip, expectedPath);
		then(publicKeysSharingService).should().downloadPublicKeys(expectedSessionId);
	}
}