/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class PemConverterService {

	static final String CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----";
	static final String CERTIFICATE_FOOTER = "-----END CERTIFICATE-----";
	private final CertificateFactory certFactory;

	public PemConverterService() throws CertificateException {
		certFactory = CertificateFactory.getInstance("X.509");
	}

	public X509Certificate fromPem(final String certificateAsPem) {
		checkNotNull(certificateAsPem);
		return Optional.of(certificateAsPem)
				.map(s -> s.replace(CERTIFICATE_HEADER, ""))
				.map(s -> s.replace(CERTIFICATE_FOOTER, ""))
				.map(s -> s.replaceAll("\\s", ""))
				.map(s -> Base64.getDecoder().decode(s))
				.map(bytes -> {
					try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
						return (X509Certificate) certFactory.generateCertificate(inputStream);
					} catch (final CertificateException | IOException e) {
						throw new PemConverterException(e);
					}
				}).orElseThrow();
	}

	public String toPem(final Certificate certificate) {
		checkNotNull(certificate);

		try {
			final Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8));

			final StringBuilder builder = new StringBuilder();
			builder.append(CERTIFICATE_HEADER);
			builder.append("\n");
			builder.append(new String(encoder.encode(certificate.getEncoded())));
			builder.append("\n");
			builder.append(CERTIFICATE_FOOTER);

			return builder.toString();

		} catch (final CertificateEncodingException e) {
			throw new PemConverterException(e);
		}
	}

	public static class PemConverterException extends RuntimeException {
		public PemConverterException(final Throwable cause) {
			super(cause);
		}
	}
}
