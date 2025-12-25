/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.integrationtests;

import static java.lang.String.format;
import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ch.post.it.evoting.directtrusttool.backend.session.Phase;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService;
import ch.post.it.evoting.directtrusttool.cli.DirectTrustToolCliApplication;
import ch.post.it.evoting.evotinglibraries.direct.trust.KeystoreValidator;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@SpringBootTest(webEnvironment = NONE, classes = DirectTrustToolCliApplication.class)
@ActiveProfiles("test")
public class DirectTrustToolCliApplicationIT {

	@Value("${app.directory.output}")
	Path sessionDirectory;

	@TempDir
	Path tempDir;

	@Autowired
	DirectTrustToolCliApplication application;

	@Autowired
	SessionService sessionService;

	@Test
	void testHappyPath() throws IOException, InterruptedException {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String sessionId = uuidGenerator.generate();

		// DTT process
		keystoreGeneration(sessionId);
		downloadPublicKeys(sessionId);
		importPublicKeys(sessionId);
		downloadKeystores(sessionId);
		clear(sessionId);

		// Validation of the key stores
		validateKeystores();
	}

	private void keystoreGeneration(final String sessionId) {
		// precondition
		assertEquals(Phase.KEYSTORES_GENERATION, sessionService.getSessionPhase(sessionId));

		// given
		final String components = Arrays.stream(Alias.values()).map(Alias::get).reduce((a, b) -> a + "," + b).orElse("");
		final LocalDate date = LocalDateUtils.now().plusDays(1);
		final String country = "testCountry";
		final String state = "Aargau";
		final String locality = "testLocality";
		final String organization = "DT_CT_20001212_TT01";
		final String platform = "integration";

		// when
		application.run(
				"keystores-generation",
				"--session-id", sessionId,
				"--components", components,
				"--valid-until", date.format(DateTimeFormatter.ISO_LOCAL_DATE),
				"--country", country,
				"--state", state,
				"--locality", locality,
				"--organization", organization,
				"--platform", platform
		);

		// then
		assertTrue(Files.exists(sessionDirectory));
		assertTrue(Files.exists(sessionDirectory.resolve(sessionId)));
		assertEquals(Phase.PUBLIC_KEYS_SHARING, sessionService.getSessionPhase(sessionId));
	}

	private void downloadPublicKeys(final String sessionId) throws IOException {
		// precondition
		assertEquals(Phase.PUBLIC_KEYS_SHARING, sessionService.getSessionPhase(sessionId));

		// given
		final Path publicKeysZip = tempDir.resolve("pub_keys.zip");
		final long expectedCount = Arrays.stream(Alias.values()).filter(Alias::hasPrivateKey).count();

		// when / then
		application.run(
				"public-keys-sharing-download",
				"--session-id", sessionId,
				"--output", publicKeysZip.toString()
		);
		assertTrue(Files.exists(publicKeysZip));

		unzip(publicKeysZip, tempDir.resolve("pub_keys"));

		assertTrue(Files.exists(tempDir.resolve("pub_keys")));
		try (final Stream<Path> list = list(tempDir.resolve("pub_keys"))) {
			assertEquals(expectedCount, list.count());
		}
		assertEquals(Phase.PUBLIC_KEYS_SHARING, sessionService.getSessionPhase(sessionId));
	}

	private void importPublicKeys(final String sessionId) {
		// precondition
		assertEquals(Phase.PUBLIC_KEYS_SHARING, sessionService.getSessionPhase(sessionId));

		// givens
		final Path publicKeysPath = tempDir.resolve("pub_keys");

		// when
		application.run(
				"public-keys-sharing-import",
				"--session-id", sessionId,
				"--public-key-path", publicKeysPath.toString()
		);

		// then
		assertTrue(Files.exists(sessionDirectory));
		assertTrue(Files.exists(sessionDirectory.resolve(sessionId)));
		assertEquals(Phase.KEYSTORES_DOWNLOAD, sessionService.getSessionPhase(sessionId));
	}

	private void downloadKeystores(final String sessionId) throws IOException {
		// precondition
		assertEquals(Phase.KEYSTORES_DOWNLOAD, sessionService.getSessionPhase(sessionId));

		// givens
		final Path keystoreZip = tempDir.resolve("test_keystores.zip");

		// when
		application.run(
				"keystores-download",
				"--session-id", sessionId,
				"--output", keystoreZip.toString()
		);

		// then
		assertTrue(Files.exists(keystoreZip));

		unzip(keystoreZip, tempDir.resolve("test_keystores"));
		try (final Stream<Path> components = list(tempDir.resolve("test_keystores"))) {
			assertEquals(Alias.values().length, components.count());
		}
		try (final Stream<Path> components = list(tempDir.resolve("test_keystores"))) {
			components.forEach(path -> {
				try (final Stream<Path> contents = list(path)) {
					assertEquals(Alias.getByComponentName(path.getFileName().toString()).hasPrivateKey() ? 3 : 2, contents.count());
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			});
		}

		assertEquals(Phase.KEYSTORES_DOWNLOAD, sessionService.getSessionPhase(sessionId));
	}

	private void clear(final String sessionId) throws IOException, InterruptedException {
		// precondition
		assertEquals(Phase.KEYSTORES_DOWNLOAD, sessionService.getSessionPhase(sessionId));

		// when
		application.run(
				"clear",
				"--session-id", sessionId
		);

		// then
		try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
			sessionDirectory.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
			watchService.poll(5, TimeUnit.SECONDS);
		}
		assertTrue(Files.exists(sessionDirectory));
		assertTrue(Files.notExists(sessionDirectory.resolve(sessionId)));
		assertEquals(Phase.KEYSTORES_GENERATION, sessionService.getSessionPhase(sessionId));
	}

	private void validateKeystores() {
		// given
		final Path keystoresPath = tempDir.resolve("test_keystores");
		Arrays.stream(Alias.values())
				.forEach(alias -> {
					final Path keystorePath = keystoresPath.resolve(alias.get());
					try {
						final char[] password = Files.readString(keystorePath.resolve(format("integration_direct_trust_pw_%s.txt", alias.get())))
								.toCharArray();
						final KeyStore keystore = KeyStore.getInstance(
								keystorePath.resolve(format("integration_direct_trust_keystore_%s.p12", alias.get())).toFile(), password);
						final Alias signingAlias = alias.hasPrivateKey() ? alias : null;
						assertTrue(KeystoreValidator.validateKeystore(keystore, signingAlias, password).isVerified());
					} catch (final KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
						throw new RuntimeException(e);
					}
				});
	}

	public static void unzip(final Path zipFilePath, final Path targetDir) throws IOException {
		try (final ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath))) {
			for (ZipEntry entry; (entry = zis.getNextEntry()) != null; ) {
				final Path resolvedPath = targetDir.resolve(entry.getName()).normalize();
				Files.createDirectories(resolvedPath.getParent());
				Files.copy(zis, resolvedPath);
			}
		}
	}
}
