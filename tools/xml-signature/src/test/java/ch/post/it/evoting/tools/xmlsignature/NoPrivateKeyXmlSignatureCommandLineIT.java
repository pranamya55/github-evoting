/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.CONFIGURATION_ANONYMIZED_INVALID_SIGNATURE_XML_PATH;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.ERROR_LOG_PREFIX;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.EVOTING_PRINT_INVALID_SIGNATURE_XML_PATH;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.FAILURE_LOG_PREFIX;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.SUCCESS_LOG_PREFIX;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.getConfigurationAnonymizedPath;
import static ch.post.it.evoting.tools.xmlsignature.XmlSignatureCommandLineUtils.getEvotingPrintPath;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringBootTest(properties = {
		"direct-trust.keystore.location=target/test-classes/direct-trust/verifier/local_direct_trust_keystore_verifier.p12",
		"direct-trust.password.location=target/test-classes/direct-trust/verifier/local_direct_trust_pw_verifier.txt"
})
@DisplayName("XmlSignatureCommandLine with keystore without private key calling")
class NoPrivateKeyXmlSignatureCommandLineIT {

	private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(XmlSignatureCommandLine.class);
	private static final Path CONFIGURATION_ANONYMIZED_PATH = getConfigurationAnonymizedPath();
	private static final Path EVOTING_PRINT_PATH = getEvotingPrintPath();

	@Autowired
	private XmlSignatureCommandLine xmlSignatureCommandLine;

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		logAppender = new ListAppender<>();
		logAppender.start();
		LOGGER.addAppender(logAppender);
	}

	@Test
	@DisplayName("sign logs error")
	void signWithKeystoreWithoutSigningAliasLogsError() {
		xmlSignatureCommandLine.run(SupportedFileType.CONFIG.name(), Action.SIGN.name(), CONFIGURATION_ANONYMIZED_PATH.toString());
		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(ERROR_LOG_PREFIX)));
	}

	@Test
	@DisplayName("verify with valid configuration-anonymized signature logs success")
	void verifyValidConfigurationAnonymizedSignatureLogsSuccess() {
		xmlSignatureCommandLine.run(SupportedFileType.CONFIG.name(), Action.VERIFY.name(), CONFIGURATION_ANONYMIZED_PATH.toString());
		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(SUCCESS_LOG_PREFIX)));
	}

	@Test
	@DisplayName("verify with valid evoting-print signature logs success")
	void verifyValidEvotingPrintLogsSuccess() {
		xmlSignatureCommandLine.run(SupportedFileType.PRINT.name(), Action.VERIFY.name(), EVOTING_PRINT_PATH.toString());
		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(SUCCESS_LOG_PREFIX)));
	}

	@Test
	@DisplayName("verify with invalid configuration-anonymized signature logs failure")
	void verifyInvalidSignatureConfigurationAnonymizedLogsFailure() throws URISyntaxException {
		final Path filePath = Paths.get(
				Objects.requireNonNull(this.getClass().getResource(CONFIGURATION_ANONYMIZED_INVALID_SIGNATURE_XML_PATH)).toURI());

		xmlSignatureCommandLine.run(SupportedFileType.CONFIG.name(), Action.VERIFY.name(), filePath.toString());

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(FAILURE_LOG_PREFIX)));
	}

	@Test
	@DisplayName("verify with invalid evoting-print signature logs failure")
	void verifyInvalidEvotingPrintSignatureLogsFailure() throws URISyntaxException {
		final Path filePath = Paths.get(
				Objects.requireNonNull(this.getClass().getResource(EVOTING_PRINT_INVALID_SIGNATURE_XML_PATH)).toURI());

		xmlSignatureCommandLine.run(SupportedFileType.PRINT.name(), Action.VERIFY.name(), filePath.toString());

		assertTrue(logAppender.list.stream()
				.map(ILoggingEvent::getMessage)
				.anyMatch(message -> message.startsWith(FAILURE_LOG_PREFIX)));
	}
}
