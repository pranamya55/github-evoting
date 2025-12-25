/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.session;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.cryptoprimitives.math.RandomFactory.createRandom;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionIdValidator.validateSessionId;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.cryptoprimitives.math.Base16Alphabet;
import ch.post.it.evoting.directtrusttool.backend.ResetMode;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@Service
public class SessionService {
	private static final Logger LOGGER = getLogger(SessionService.class);

	private static final String SESSION_PHASE_KEY = "phase";
	private static final String PROPERTY_FILE = "session.properties";

	private final FileRepository fileRepository;
	private final ObjectMapper mapper;
	private final boolean generateKeystores;
	private final ResetMode resetMode;

	@Autowired
	public SessionService(
			final FileRepository fileRepository,
			final ObjectMapper mapper,
			@Value("${app.component.available:}")
			final List<String> availableComponents,
			@Value("${app.resetMode:#{T(ch.post.it.evoting.directtrusttool.backend.ResetMode).getDefaultResetMode()}}")
			final ResetMode resetMode) {
		this.fileRepository = fileRepository;
		this.mapper = mapper;
		this.generateKeystores = !availableComponents.isEmpty();
		this.resetMode = resetMode;
	}

	/**
	 * Constructor use by the CLI interface.
	 */
	public SessionService(final FileRepository fileRepository, final ObjectMapper mapper) {
		this.fileRepository = fileRepository;
		this.mapper = mapper;
		this.generateKeystores = true;
		this.resetMode = ResetMode.ALWAYS_ENABLED;
	}


	public String createNewSession() {
		return createRandom().genRandomString(ID_LENGTH, Base16Alphabet.getInstance());
	}

	public Phase getSessionPhase(final String sessionId) {
		validateSessionId(sessionId);

		final Phase defaultPhase = generateKeystores ? Phase.KEYSTORES_GENERATION : Phase.PUBLIC_KEYS_SHARING;

		return Optional.ofNullable(getGlobalStorageKey(sessionId, SESSION_PHASE_KEY))
				.map(Phase::valueOf)
				.orElse(defaultPhase);
	}

	public void setPhase(final String sessionId, final Phase phase) {
		putGlobalStorageKey(sessionId, SESSION_PHASE_KEY, phase.name());
	}

	public void deleteSession(final String sessionId) {
		if(resetMode == ResetMode.ENABLED_AT_END && !getSessionPhase(sessionId).equals(Phase.KEYSTORES_DOWNLOAD)){
			LOGGER.warn("reset session is forbidden when not at the last phase. [currentPhase: {}]", getSessionPhase(sessionId));
			return;
		}
		if(resetMode == ResetMode.NEVER_ENABLED){
			LOGGER.warn("reset session is forbidden.");
			return;
		}

		deleteStorage(sessionId);
	}

	public ImmutableSet<Alias> getCurrentComponents(final String sessionId) {
		return selectedComponents(sessionId);
	}

	public String getSessionKey(final String sessionId, final String key) {
		return getGlobalStorageKey(sessionId, key);
	}

	public void deleteStorage(final String sessionId) {
		validateSessionId(sessionId);
		final Path sessionDirectory = Path.of(sessionId);
		fileRepository.removeDirectory(sessionDirectory);
	}

	public void putGlobalStorageKey(final String sessionId, final String key, final String value) {
		validateSessionId(sessionId);
		checkNotNull(key);
		checkNotNull(value);

		final Map<String, String> map = readGlobalMap(sessionId);
		map.put(key, value);
		writeGlobalMap(sessionId, map);
	}

	public String getGlobalStorageKey(final String sessionId, final String key) {
		validateSessionId(sessionId);
		checkNotNull(key);
		checkState(key.matches("^\\w+$"), "The key must only contain letters, numbers and underscore.");
		return readGlobalMap(sessionId).get(key);
	}

	public String getGlobalStorageKey(final String sessionId, final String key, final String defaultValue) {
		checkNotNull(defaultValue);

		return Optional.ofNullable(readGlobalMap(sessionId).get(key))
				.orElse(defaultValue);
	}

	private Map<String, String> readGlobalMap(final String sessionId) {
		final Path propertyFile = Path.of(sessionId).resolve(PROPERTY_FILE);

		return fileRepository.readFile(propertyFile)
				.map(ImmutableByteArray::elements)
				.map(bytes -> new String(bytes, StandardCharsets.UTF_8))
				.map(s -> {
					try {
						return mapper.readValue(s, new TypeReference<HashMap<String, String>>() {
						});
					} catch (final JsonProcessingException e) {
						throw new ComponentStorageRepositoryException(e);
					}
				})
				.orElse(new HashMap<>());
	}

	private void writeGlobalMap(final String sessionId, final Map<String, String> map) {
		final Path propertyFile = Path.of(sessionId).resolve(PROPERTY_FILE);
		try {
			fileRepository.writeFile(propertyFile, new ImmutableByteArray(mapper.writeValueAsBytes(map)));
		} catch (final JsonProcessingException e) {
			throw new ComponentStorageRepositoryException(e);
		}
	}

	public ImmutableSet<Alias> selectedComponents(final String sessionId) {
		validateSessionId(sessionId);
		return Arrays.stream(getGlobalStorageKey(sessionId, "selected_component", "").split(","))
				.filter(s -> !s.isBlank())
				.map(Alias::getByComponentName)
				.collect(toImmutableSet());
	}

	public Optional<char[]> getCharArray(final Key key) {
		return getBytes(key)
				.map(ImmutableByteArray::elements)
				.map(ByteBuffer::wrap)
				.map(StandardCharsets.UTF_8::decode)
				.map(charBuffer -> Arrays.copyOfRange(charBuffer.array(), charBuffer.arrayOffset(), charBuffer.limit()));
	}

	public Optional<ImmutableByteArray> getBytes(final Key key) {
		return fileRepository.readFile(getKeyLocation(key));
	}

	public void putCharArray(final Key key, final char[] value) {
		final ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(value));
		putBytes(key, new ImmutableByteArray(Arrays.copyOfRange(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.limit())));
	}

	public void putBytes(final Key key, final ImmutableByteArray value) {
		fileRepository.writeFile(getKeyLocation(key), value);
	}

	private Path getKeyLocation(final Key key) {
		return Path.of(key.sessionId).resolve(key.component.name()).resolve(key.type.getKeyLabel());
	}

	public enum Type {
		KEYSTORE("keystore", "keystore", "p12"),
		PUBLIC_KEY("public-key", "certificate", "pem"),
		PASSWORD("password", "pw", "txt");

		private final String keyLabel;
		private final String fileNameId;
		private final String extension;

		Type(final String keyLabel, final String fileNameId, final String extension) {
			this.keyLabel = keyLabel;
			this.fileNameId = fileNameId;
			this.extension = extension;
		}

		public String getKeyLabel() {
			return keyLabel;
		}

		public String getFileNameId() {
			return fileNameId;
		}

		public String getExtension() {
			return extension;
		}
	}

	public record Key(String sessionId, Alias component, Type type) {
		public Key {
			validateSessionId(sessionId);
			checkNotNull(component);
			checkNotNull(type);
		}
	}

	public static class ComponentStorageRepositoryException extends RuntimeException {
		public ComponentStorageRepositoryException(final Throwable cause) {
			super(cause);
		}
	}
}
