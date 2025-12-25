/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process;

import static com.google.common.base.Preconditions.checkNotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.HexFormat;
import java.util.Locale;

public class CertificateHashCalculator {

	private CertificateHashCalculator() {
		// utility class
	}

	public static String calculateFingerprintForCertificate(final Certificate certificate) {
		checkNotNull(certificate);

		final byte[] encodedCertificate;
		try {
			encodedCertificate = certificate.getEncoded();
		} catch (final CertificateEncodingException e) {
			throw new IllegalStateException("Failed to get encoded certificate.", e);
		}

		final MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException("Failed to get message digest instance.", e);
		}

		final byte[] digest = messageDigest.digest(encodedCertificate);
		final String sha256Fingerprint = HexFormat.of().formatHex(digest);

		return sha256Fingerprint.toLowerCase(Locale.ENGLISH);
	}
}
