/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.api.v1;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.directtrusttool.backend.api.v1.RouteConstants.BASE_PATH;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionIdValidator.validateSessionId;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartRequest;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.directtrusttool.backend.process.NameService;
import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.CertificatesDownloadDto;
import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.PublicKeysSharingService;

@RestController
@RequestMapping(BASE_PATH + "/public-keys")
public class PublicKeysSharingController {

	private final PublicKeysSharingService publicKeysSharingService;
	private final NameService nameService;

	public PublicKeysSharingController(final PublicKeysSharingService publicKeysSharingService, final NameService nameService) {
		this.publicKeysSharingService = publicKeysSharingService;
		this.nameService = nameService;
	}

	@GetMapping(value = "{sessionId}")
	public CertificatesDownloadDto downloadPublicKeys(
			@PathVariable
			final String sessionId) {
		validateSessionId(sessionId);
		return new CertificatesDownloadDto(
				nameService.getArchiveName(sessionId),
				new String(Base64.getEncoder().encode(publicKeysSharingService.downloadPublicKeys(sessionId).elements()))
		);
	}

	@PostMapping(value = "{sessionId}", consumes = "multipart/form-data")
	public void importPublicKeys(
			@PathVariable
			final String sessionId,
			final MultipartRequest multipartRequest) {
		validateSessionId(sessionId);
		checkNotNull(multipartRequest);

		final ImmutableMap<String, String> componentKeys = multipartRequest.getFileMap().entrySet().stream()
				.map(kv -> ImmutableMap.entry(kv.getKey(), kv.getValue()))
				.collect(toImmutableMap(ImmutableMap.Entry::key, kv -> {
					try {
						return new String(kv.value().getBytes(), StandardCharsets.UTF_8);
					} catch (final IOException e) {
						throw new UncheckedIOException(e);
					}
				}));

		publicKeysSharingService.importPublicKeys(sessionId, componentKeys);
	}

	@GetMapping(value = "fingerprints/{sessionId}", produces = "application/json")
	public ImmutableMap<String, String> getFingerprints(
			@PathVariable
			final String sessionId
	) {
		validateSessionId(sessionId);
		return publicKeysSharingService.extractFingerprints(sessionId);
	}
}
