/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.directtrusttool.backend.session.SessionService;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService.Type;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@Service
public final class NameService {

	private static final String DIRECT_TRUST = "direct_trust";
	private static final String ARCHIVE_EXTENSION = "zip";
	private static final Pattern EXTRACTION_PATTERN = Pattern.compile(
			"^(?<platform>[a-z]*)_?" +
					DIRECT_TRUST + "_" +
					"(?<fileNameId>[a-z]+)" + "_" +
					"(?<name>\\w+)" + "\\." +
					"(?<extension>\\w+)$");

	private final SessionService sessionService;

	public NameService(final SessionService sessionService) {
		this.sessionService = sessionService;
	}

	public  Alias getAliasFromFileName(final String name) {
		checkNotNull(name);
		final Matcher matcher = EXTRACTION_PATTERN.matcher(name);
		if (matcher.matches()) {
			final String controlComponentName = matcher.group("name");
			return Alias.getByComponentName(controlComponentName);
		}
		throw new NoSuchElementException(format("Cannot extract the component name from the name. [name: %s]", name));
	}

	public String getFileName(final String sessionId, final Alias component, final Type type) {
		checkNotNull(sessionId);
		checkNotNull(component);
		checkNotNull(type);
		final String platform = sessionService.getGlobalStorageKey(sessionId, "platform");
		return format("%s_%s_%s_%s.%s", platform, DIRECT_TRUST, type.getFileNameId(), component.get(), type.getExtension()).replaceAll("^_", "");
	}

	public String getArchiveName(final String sessionId) {
		final String seed = sessionService.getGlobalStorageKey(sessionId, "seed");
		final String label = switch (sessionService.getSessionPhase(sessionId)) {
			case PUBLIC_KEYS_SHARING -> "certificates";
			case KEYSTORES_DOWNLOAD -> "keystores";
			default -> throw new IllegalStateException("Unexpected value: " + sessionService.getSessionPhase(sessionId));
		};

		return format("%s_%s_%s.%s", seed, DIRECT_TRUST, label, ARCHIVE_EXTENSION);
	}
}
