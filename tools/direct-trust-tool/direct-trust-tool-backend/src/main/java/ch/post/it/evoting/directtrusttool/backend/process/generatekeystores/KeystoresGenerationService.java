/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.generatekeystores;

import static ch.post.it.evoting.directtrusttool.backend.session.SessionIdValidator.validateSessionId;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.signing.AuthorityInformation;
import ch.post.it.evoting.directtrusttool.backend.process.PemConverterService;
import ch.post.it.evoting.directtrusttool.backend.session.Phase;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService.Key;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService.Type;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@Service
public class KeystoresGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(KeystoresGenerationService.class);
	private final SessionService sessionService;
	private final PemConverterService pemConverterService;

	public KeystoresGenerationService(
			final SessionService sessionService,
			final PemConverterService pemConverterService) {
		this.sessionService = sessionService;
		this.pemConverterService = pemConverterService;
	}

	public void generateKeystores(final String sessionId, final KeystorePropertiesDto properties) {
		validateSessionId(sessionId);
		checkNotNull(properties);
		checkState(sessionService.getSessionPhase(sessionId).equals(Phase.KEYSTORES_GENERATION));

		sessionService.putGlobalStorageKey(sessionId, "selected_component", properties.wantedComponents().stream()
				.map(Alias::get)
				.collect(Collectors.joining(",")));

		LOGGER.info("Storage created successfully. [sessionId: {}]", sessionId);

		final KeystoreCreator keystoreCreator = new KeystoreCreator(properties.validUntil(),
				AuthorityInformation.builder()
						.setCommonName("") // the name will be overwritten with component name
						.setCountry(properties.country())
						.setState(properties.state())
						.setLocality(properties.locality())
						.setOrganisation(properties.organisation())
						.build());

		sessionService.putGlobalStorageKey(sessionId, "platform", properties.platform());
		sessionService.putGlobalStorageKey(sessionId, "seed", properties.organisation());

		sessionService.selectedComponents(sessionId).stream().parallel()
				.forEach(component -> {
					try (final KeystoreCreator.Output output = keystoreCreator.generateKeystore(component)) {
						sessionService.putCharArray(
								new Key(sessionId, component, Type.PASSWORD),
								output.password().get());
						sessionService.putBytes(
								new Key(sessionId, component, Type.KEYSTORE),
								output.keyStore());
						Optional.ofNullable(output.publicKey())
								.ifPresent(bytes -> sessionService.putBytes(
										new Key(sessionId, component, Type.PUBLIC_KEY),
										new ImmutableByteArray(pemConverterService.toPem(bytes).getBytes(StandardCharsets.UTF_8))));
						LOGGER.info("component {} end saving", component);
					}
				});

		LOGGER.info("Generated keystore successfully. [sessionId: {}]", sessionId);

		sessionService.setPhase(sessionId, Phase.PUBLIC_KEYS_SHARING);
	}
}
