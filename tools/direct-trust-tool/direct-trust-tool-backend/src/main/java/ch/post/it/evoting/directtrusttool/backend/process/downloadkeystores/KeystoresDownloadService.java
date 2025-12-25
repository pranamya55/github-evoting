/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.downloadkeystores;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionIdValidator.validateSessionId;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionService.Type.KEYSTORE;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionService.Type.PASSWORD;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionService.Type.PUBLIC_KEY;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.directtrusttool.backend.process.NameService;
import ch.post.it.evoting.directtrusttool.backend.process.Zipper;
import ch.post.it.evoting.directtrusttool.backend.session.Phase;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@Service
public class KeystoresDownloadService {

	public static final String ZIP_PATH_SEPARATOR = "/";

	private static final Logger LOGGER = LoggerFactory.getLogger(KeystoresDownloadService.class);
	private final SessionService sessionService;
	private final NameService nameService;

	public KeystoresDownloadService(
			final SessionService sessionService,
			final NameService nameService) {
		this.sessionService = sessionService;
		this.nameService = nameService;
	}

	public ImmutableByteArray downloadKeystores(final String sessionId) {
		validateSessionId(sessionId);
		checkState(sessionService.getSessionPhase(sessionId).equals(Phase.KEYSTORES_DOWNLOAD));

		final String platform = sessionService.getGlobalStorageKey(sessionId, "platform");
		final ImmutableMap<String, ImmutableByteArray> filesMap = sessionService.selectedComponents(sessionId).stream()
				.flatMap(
						component -> {
							ImmutableList<ImmutableMap.Entry<String, ImmutableByteArray>> element = ImmutableList.of(
									createDownloableEntry(sessionId, component, KEYSTORE),
									createDownloableEntry(sessionId, component, PASSWORD));

							if (component.hasPrivateKey()) {
								element = element.append(createDownloableEntry(sessionId, component, PUBLIC_KEY));
							}
							return element.stream();
						})
				.collect(toImmutableMap());

		LOGGER.info("Files created successfully. Ready to create zip and download keystore. [sessionId: {}, platform: {}]", sessionId, platform);

		return Zipper.zip(filesMap);
	}

	private ImmutableMap.Entry<String, ImmutableByteArray> createDownloableEntry(final String sessionId, final Alias component,
			final SessionService.Type type) {
		final String folderName = component.get() + ZIP_PATH_SEPARATOR;
		final ImmutableByteArray data = sessionService.getBytes(new SessionService.Key(sessionId, component, type))
				.orElseThrow();
		return ImmutableMap.entry(folderName + nameService.getFileName(sessionId, component, type), data);
	}
}
