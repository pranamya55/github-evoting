/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.CANTON_KEYSTORE_PASSWORD_PATH;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.CANTON_KEYSTORE_PATH;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.CONFIGURATION_ANONYMIZED_XML;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.ERROR_LOG_PREFIX;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.EVOTING_PRINT_XML;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.FAILURE_LOG_PREFIX;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.SDM_KEYSTORE_PASSWORD_PATH;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.SDM_KEYSTORE_PATH;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.SUCCESS_LOG_PREFIX;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.USAGE_ERROR_LOG_PREFIX;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.getConfigurationAnonymizedPath;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.getEvotingPrintPath;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.getKeystorePasswordPath;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.getKeystorePath;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.tools.xmlsignature.keystore.KeystoreRepository;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class XmlSignatureCommandLineTest {

	private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(XmlSignatureCommandLine.class);
	private static final Path CONFIGURATION_ANONYMIZED_PATH = getConfigurationAnonymizedPath();
	private static final Path EVOTING_PRINT_PATH = getEvotingPrintPath();
	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path signFolder;
	private final KeystoreRepository keystoreRepository = new KeystoreRepository(
			getKeystorePath(SDM_KEYSTORE_PATH).toString(),
			XmlSignatureCommandLineUtils.getKeystorePasswordPath(SDM_KEYSTORE_PASSWORD_PATH).toString());
	private final XmlSignatureCommandLine xmlSignatureCommandLine = spy(new XmlSignatureCommandLine(keystoreRepository));
	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		logAppender = new ListAppender<>();
		logAppender.start();
		LOGGER.addAppender(logAppender);
	}

	@Test
	void runWithUnknownSignerLogsError() {
		xmlSignatureCommandLine.run("unknown", Action.VERIFY.name(), "path/to/file");

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(USAGE_ERROR_LOG_PREFIX)));
	}

	@Test
	void runWithUnknownActionLogsError() {
		xmlSignatureCommandLine.run(SupportedFileType.CONFIG.name(), "unknown", "path/to/file");

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(USAGE_ERROR_LOG_PREFIX)));
	}

	@Test
	void runWithTooManyArgumentsLogsError() {
		xmlSignatureCommandLine.run(SupportedFileType.CONFIG.name(), Action.VERIFY.name(), "path/to/file", "extra-argument");

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(USAGE_ERROR_LOG_PREFIX)));
	}

	@Test
	void runWithTwoArgumentsLogsError() {
		xmlSignatureCommandLine.run(SupportedFileType.CONFIG.name(), Action.VERIFY.name());

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(USAGE_ERROR_LOG_PREFIX)));
	}

	@Test
	void runWithOneArgumentLogsError() {
		xmlSignatureCommandLine.run(SupportedFileType.CONFIG.name());

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(USAGE_ERROR_LOG_PREFIX)));
	}

	@Test
	void runWithNoArgumentsLogsError() {
		xmlSignatureCommandLine.run();

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(USAGE_ERROR_LOG_PREFIX)));
	}

	@Test
	void signWithSignatureExceptionLogsError() {
		xmlSignatureCommandLine.run(SupportedFileType.CONFIG.name(), Action.SIGN.name(), CONFIGURATION_ANONYMIZED_PATH.toString());

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(ERROR_LOG_PREFIX)));
	}

	private static Stream<Arguments> signArgumentProvider() {
		return Stream.of(
				Arguments.of(SupportedFileType.CONFIG, Action.SIGN, CONFIGURATION_ANONYMIZED_XML, SUCCESS_LOG_PREFIX),
				Arguments.of(SupportedFileType.CONFIG, Action.SIGN, EVOTING_PRINT_XML, ERROR_LOG_PREFIX),
				Arguments.of(SupportedFileType.PRINT, Action.SIGN, CONFIGURATION_ANONYMIZED_XML, ERROR_LOG_PREFIX),
				Arguments.of(SupportedFileType.PRINT, Action.SIGN, EVOTING_PRINT_XML, SUCCESS_LOG_PREFIX)
		);
	}

	@ParameterizedTest
	@MethodSource("signArgumentProvider")
	void signTest(final SupportedFileType signer, final Action action, final String xml, final String expectedLog)
			throws Exception {
		final KeystoreRepository testKeystoreRepository;
		if (xml.equals(EVOTING_PRINT_XML)) {
			testKeystoreRepository = new KeystoreRepository(getKeystorePath(SDM_KEYSTORE_PATH).toString(),
					getKeystorePasswordPath(SDM_KEYSTORE_PASSWORD_PATH).toString());
		} else {
			testKeystoreRepository = new KeystoreRepository(getKeystorePath(CANTON_KEYSTORE_PATH).toString(),
					getKeystorePasswordPath(CANTON_KEYSTORE_PASSWORD_PATH).toString());
		}
		final XmlSignatureCommandLine testXmlSignatureCommandLine = new XmlSignatureCommandLine(testKeystoreRepository);

		Files.copy(CONFIGURATION_ANONYMIZED_PATH, signFolder.resolve(CONFIGURATION_ANONYMIZED_XML), REPLACE_EXISTING);
		Files.copy(EVOTING_PRINT_PATH, signFolder.resolve(EVOTING_PRINT_XML), REPLACE_EXISTING);

		final Path filePath = signFolder.resolve(xml);

		testXmlSignatureCommandLine.run(signer.name(), action.name(), filePath.toString());

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(expectedLog)));
	}

	private static Stream<Arguments> verifyArgumentProvider() {
		return Stream.of(
				Arguments.of(SupportedFileType.CONFIG, Action.VERIFY, CONFIGURATION_ANONYMIZED_PATH, SUCCESS_LOG_PREFIX),
				Arguments.of(SupportedFileType.CONFIG, Action.VERIFY, EVOTING_PRINT_PATH, FAILURE_LOG_PREFIX),
				Arguments.of(SupportedFileType.PRINT, Action.VERIFY, CONFIGURATION_ANONYMIZED_PATH, FAILURE_LOG_PREFIX),
				Arguments.of(SupportedFileType.PRINT, Action.VERIFY, EVOTING_PRINT_PATH, SUCCESS_LOG_PREFIX)
		);
	}

	@ParameterizedTest
	@MethodSource("verifyArgumentProvider")
	void verifyTest(final SupportedFileType signer, final Action action, final Path xmlPath, final String expectedLog) {

		xmlSignatureCommandLine.run(signer.name(), action.name(), xmlPath.toString());

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(expectedLog)));
	}
}
