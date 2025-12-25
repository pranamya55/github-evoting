/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import java.nio.file.Path;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ch.post.it.evoting.tools.xmlsignature.keystore.KeystoreRepository;

@Component
public class XmlSignatureCommandLine {

	private static final Logger LOGGER = LoggerFactory.getLogger(XmlSignatureCommandLine.class);
	private static final String USAGE_TEXT = "Usage : java -Ddirect-trust.keystore.location=<direct-trust-keystoreFile> "
			+ "-Ddirect-trust.password.location=<direct-trust-passFile> -jar xml-signature.jar <"
			+ String.join("|", Arrays.stream(SupportedFileType.values()).map(SupportedFileType::name).toList())
			+ "> <"
			+ String.join("|", Arrays.stream(Action.values()).map(Action::name).toList())
			+ "> <filePath>";

	private final KeystoreRepository keystoreRepository;

	public XmlSignatureCommandLine(final KeystoreRepository keystoreRepository) {
		this.keystoreRepository = keystoreRepository;
	}

	public void run(final String... args) {
		final boolean isExistingFileType = args.length > 0 && Arrays.stream(SupportedFileType.values()).anyMatch(s -> s.name().equals(args[0]));
		final boolean isExistingAction = args.length > 1 && Arrays.stream(Action.values()).anyMatch(a -> a.name().equals(args[1]));
		if (!isExistingFileType || !isExistingAction || args.length != 3) {
			LOGGER.error(USAGE_TEXT);
			new Result(false, USAGE_TEXT).logAsJson();
			return;
		}

		try {
			// Extract arguments.
			final SupportedFileType fileType = SupportedFileType.valueOf(args[0]);
			final Action action = Action.valueOf(args[1]);
			final Path filePath = Path.of(args[2]);
			final XmlSigner xmlSigner = new XmlSigner();

			// Apply action by signer.
			final Result result = action.apply(xmlSigner, filePath, keystoreRepository, fileType.getSigningAlias());

			result.logAsJson();

			if (result.signatureVerified()) {
				LOGGER.info("The action has finished successfully. [fileType: {}, action: {}, filePath: {}]", fileType, action, filePath);
			} else {
				LOGGER.error("The action failed. [fileType: {}, action: {}, filePath: {}]", fileType, action, filePath);
			}
		} catch (final Exception e) {
			LOGGER.error("Unable to process the requested action.", e);
			new Result(false, e.getMessage()).logAsJson();
		}
	}

}
