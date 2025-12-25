/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.PublicKeysSharingService;
import ch.post.it.evoting.directtrusttool.cli.DirectTrustToolCliApplication;

import picocli.CommandLine;

@SpringBootTest(webEnvironment = NONE, classes = DirectTrustToolCliApplication.class)
class PublicKeysFingerprintCommandTest {

	@MockitoBean
	PublicKeysSharingService publicKeysSharingService;

	@Autowired
	CommandLine.IFactory factory;

	@Autowired
	PublicKeysFingerprintCommand publicKeysFingerprintCommand;

	@AfterEach
	void tearDown() {
		Mockito.reset(publicKeysSharingService);
	}

	@Test
	void testPublicKeysFingerprintWithDefaultSession() {
		// given
		final String expectedSessionId = "00000000000000000000000000000000";

		given(publicKeysSharingService.extractFingerprints(expectedSessionId)).willReturn(ImmutableMap.emptyMap());

		// when
		final int exitCode = new CommandLine(publicKeysFingerprintCommand, factory).execute();

		// then
		assertEquals(0, exitCode);
		then(publicKeysSharingService).should().extractFingerprints(expectedSessionId);
	}

	@Test
	void testPublicKeysFingerprintWithCustomSession() {
		// given
		final String expectedSessionId = "11111111111111111111111111111111";

		given(publicKeysSharingService.extractFingerprints(expectedSessionId)).willReturn(ImmutableMap.emptyMap());

		// when
		final int exitCode = new CommandLine(publicKeysFingerprintCommand, factory)
				.execute("--session-id", expectedSessionId);

		// then
		assertEquals(0, exitCode);
		then(publicKeysSharingService).should().extractFingerprints(expectedSessionId);
	}
}
