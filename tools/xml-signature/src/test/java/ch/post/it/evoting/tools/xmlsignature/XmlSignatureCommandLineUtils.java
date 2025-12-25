/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.MoreCollectors;

public class XmlSignatureCommandLineUtils {

	// ////////////////////////////////////
	//
	// files
	//
	// ////////////////////////////////////
	public static final String CONFIGURATION_ANONYMIZED_XML = "configuration-anonymized.xml";
	public static final String EVOTING_PRINT_XML = "evoting-print.xml";
	public static final Pattern EVOTING_PRINT_PATTERN = Pattern.compile("evoting-print_.*\\.xml");
	public static final String SDM_KEYSTORE_PATH = "direct-trust/secure-data-manager-setup/local_direct_trust_keystore_sdm_config.p12";
	public static final String SDM_KEYSTORE_PASSWORD_PATH = "direct-trust/secure-data-manager-setup/local_direct_trust_pw_sdm_config.txt";
	public static final String CANTON_KEYSTORE_PATH = "direct-trust/canton/local_direct_trust_keystore_canton.p12";
	public static final String CANTON_KEYSTORE_PASSWORD_PATH = "direct-trust/canton/local_direct_trust_pw_canton.txt";

	// ////////////////////////////////////
	//
	// paths
	//
	// ////////////////////////////////////
	public static final String CONFIGURATION_ANONYMIZED_INVALID_SIGNATURE_XML_PATH = "/configuration-anonymized-invalid-signature.xml";
	public static final String EVOTING_PRINT_INVALID_SIGNATURE_XML_PATH = "/evoting-print-invalid-signature.xml";

	// ////////////////////////////////////
	//
	// log messages
	//
	// ////////////////////////////////////
	public static final String USAGE_ERROR_LOG_PREFIX = "Usage : java -Ddirect-trust.keystore.location=<direct-trust-keystoreFile> ";
	public static final String SUCCESS_LOG_PREFIX = "The action has finished successfully.";
	public static final String ERROR_LOG_PREFIX = "Unable to process the requested action.";
	public static final String FAILURE_LOG_PREFIX = "The action failed.";

	private XmlSignatureCommandLineUtils() {
		// Intentionally left blank.
	}

	public static Path getKeystorePath(final String keystorePath) {
		try {
			return Paths.get(Objects.requireNonNull(XmlSignatureCommandLineUtils.class.getResource("/" + keystorePath)).toURI());
		} catch (final URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Path getKeystorePasswordPath(final String keystorePasswordPath) {
		try {
			return Paths.get(Objects.requireNonNull(XmlSignatureCommandLineUtils.class.getResource("/" + keystorePasswordPath)).toURI());
		} catch (final URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Path getConfigurationAnonymizedPath() {
		try {
			return Paths.get(Objects.requireNonNull(XmlSignatureCommandLineUtils.class.getResource("/" + CONFIGURATION_ANONYMIZED_XML)).toURI());
		} catch (final URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Path getEvotingPrintPath() {
		final Path resourceDirectory;
		try {
			resourceDirectory = Paths.get(Objects.requireNonNull(XmlSignatureCommandLineUtils.class.getResource("/")).toURI());
		} catch (final URISyntaxException e) {
			throw new IllegalStateException(e);
		}

		try (final Stream<Path> walk = Files.walk(resourceDirectory)) {
			return walk
					.filter(Files::isRegularFile)
					.filter(path -> EVOTING_PRINT_PATTERN.matcher(path.getFileName().toString()).matches())
					.collect(MoreCollectors.onlyElement());
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
