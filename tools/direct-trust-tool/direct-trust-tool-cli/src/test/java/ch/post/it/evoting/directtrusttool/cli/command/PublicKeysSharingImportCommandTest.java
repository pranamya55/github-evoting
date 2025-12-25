/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.PublicKeysSharingService;
import ch.post.it.evoting.directtrusttool.cli.DirectTrustToolCliApplication;
import ch.post.it.evoting.directtrusttool.cli.FileService;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

import picocli.CommandLine;

@SpringBootTest(webEnvironment = NONE, classes = DirectTrustToolCliApplication.class)
class PublicKeysSharingImportCommandTest {

	@MockitoBean
	PublicKeysSharingService publicKeysSharingService;
	@MockitoBean
	FileService fileService;

	@Autowired
	CommandLine.IFactory factory;

	@Autowired
	PublicKeysSharingImportCommand publicKeysSharingImportCommand;

	@AfterEach
	void tearDown() {
		Mockito.reset(publicKeysSharingService);
		Mockito.reset(fileService);
	}

	@Test
	void testImportKeysWithDefaultSession() {
		// given
		final ImmutableMap<String, String> expectedMap = Arrays.stream(Alias.values())
				.map(Alias::name)
				.collect(toImmutableMap(a -> a, a -> "content of public key for " + a));
		final String expectedSessionId = "00000000000000000000000000000000";
		final Path expectedPath = Path.of("test");

		given(fileService.getAllFilesContentAsString(any(Path.class))).willReturn(expectedMap);

		// when
		final int exitCode = new CommandLine(publicKeysSharingImportCommand, factory)
				.execute("--public-key-path", "test");

		// then
		assertEquals(0, exitCode);
		then(fileService).should().getAllFilesContentAsString(expectedPath);
		then(publicKeysSharingService).should().importPublicKeys(expectedSessionId, expectedMap);
	}

	@Test
	void testImportKeysWithCustomSession() {
		// given
		final ImmutableMap<String, String> expectedMap = Arrays.stream(Alias.values())
				.map(Alias::name)
				.collect(toImmutableMap(a -> a, a -> "content of public key for " + a));
		final String expectedSessionId = "11111111111111111111111111111111";
		final Path expectedPath = Path.of("test");

		given(fileService.getAllFilesContentAsString(any(Path.class))).willReturn(expectedMap);

		// when
		final int exitCode = new CommandLine(publicKeysSharingImportCommand, factory)
				.execute(
						"--public-key-path", "test",
						"--session-id", expectedSessionId
				);

		// then
		assertEquals(0, exitCode);
		then(fileService).should().getAllFilesContentAsString(expectedPath);
		then(publicKeysSharingService).should().importPublicKeys(expectedSessionId, expectedMap);
	}
}
