/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;

public class XmlSigner {
	private final XMLSignatureService xmlSignatureService;

	public XmlSigner() {
		this.xmlSignatureService = new XMLSignatureService();
	}

	public void sign(final Path path, final PrivateKey signingKey) {
		final Path realPath = validatePath(path);
		try (final InputStream fileInputStream = Files.newInputStream(realPath);
				final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			xmlSignatureService.genXMLSignature(fileInputStream, outputStream, signingKey);
			Files.write(realPath, outputStream.toByteArray());
		} catch (final IOException e) {
			throw new IllegalArgumentException("Could not find file to sign", e);
		}
	}

	public boolean verify(final Path path, final PublicKey signatureVerificationKey) {
		final Path realPath = validatePath(path);
		try (final InputStream fileInputStream = Files.newInputStream(realPath)) {
			return xmlSignatureService.verifyXMLSignature(fileInputStream, signatureVerificationKey);
		} catch (final IOException e) {
			throw new IllegalArgumentException("Could not find file to verify", e);
		}
	}

	private Path validatePath(final Path path) {
		checkNotNull(path);

		final Path realPath;
		try {
			realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
		} catch (final IOException e) {
			throw new IllegalStateException(String.format("The xml file does not exist or an I/O error occurred. [path: %s]", path));
		}

		final String fileName = realPath.getFileName().toString();
		checkArgument(fileName.endsWith(".xml"), "The file is not an xml. [path: %s]", realPath);

		return realPath;
	}

}
