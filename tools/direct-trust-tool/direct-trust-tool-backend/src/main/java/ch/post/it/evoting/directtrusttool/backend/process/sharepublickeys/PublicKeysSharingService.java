/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.directtrusttool.backend.process.CertificateHashCalculator.calculateFingerprintForCertificate;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionIdValidator.validateSessionId;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionService.Type.PUBLIC_KEY;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.directtrusttool.backend.process.CertificateHashCalculator;
import ch.post.it.evoting.directtrusttool.backend.process.NameService;
import ch.post.it.evoting.directtrusttool.backend.process.PemConverterService;
import ch.post.it.evoting.directtrusttool.backend.process.Zipper;
import ch.post.it.evoting.directtrusttool.backend.session.Phase;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService.Key;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService.Type;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@Service
public class PublicKeysSharingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeysSharingService.class);

	private final SessionService sessionService;
	private final PemConverterService pemConverterService;
	private final NameService nameService;

	public PublicKeysSharingService(final SessionService sessionService,
			final PemConverterService pemConverterService, final NameService nameService) {
		this.sessionService = sessionService;
		this.pemConverterService = pemConverterService;
		this.nameService = nameService;
	}

	public ImmutableByteArray downloadPublicKeys(final String sessionId) {
		validateSessionId(sessionId);
		checkState(sessionService.getSessionPhase(sessionId).equals(Phase.PUBLIC_KEYS_SHARING));

		final ImmutableMap<String, ImmutableByteArray> filesMap = sessionService.selectedComponents(sessionId).stream()
				.flatMap(component -> {
					final Key key = new Key(sessionId, component, PUBLIC_KEY);
					final String fileName = nameService.getFileName(sessionId, component, PUBLIC_KEY);
					return sessionService.getBytes(key).map(bytes -> ImmutableMap.entry(fileName, bytes)).stream();
				})
				.collect(toImmutableMap());

		LOGGER.info("Files created successfully. Ready to create zip and download public keys. [sessionId: {}]", sessionId);

		return Zipper.zip(filesMap);
	}

	public void importPublicKeys(final String sessionId, final ImmutableMap<String, String> componentKeysAsPem) {
		validateSessionId(sessionId);
		checkState(sessionService.getSessionPhase(sessionId).equals(Phase.PUBLIC_KEYS_SHARING));
		checkNotNull(componentKeysAsPem);
		checkArgument(componentKeysAsPem.size() == Alias.values().length - 1);

		// Convert the PEM map to X509 map
		final ImmutableMap<Alias, Certificate> componentKeys = componentKeysAsPem.entrySet().stream()
				.collect(toImmutableMap(
						e -> nameService.getAliasFromFileName(e.key()),
						e -> pemConverterService.fromPem(e.value())));

		// Print received value for audit
		componentKeys.entrySet().stream()
				.sorted(Comparator.comparing(ImmutableMap.Entry::key))
				.forEach(entry -> LOGGER.info("Received public key. [component: {} fingerprint: {}]", entry.key(),
						calculateFingerprintForCertificate(entry.value())));

		// Import public keys into the keystores of the owned components
		final ImmutableSet<Alias> selectedComponents = sessionService.selectedComponents(sessionId);
		selectedComponents.forEach(component -> {
			final ImmutableByteArray keystore = sessionService.getBytes(
							new Key(sessionId, component, Type.KEYSTORE))
					.orElseThrow(() -> new NoSuchElementException("Missing keystore."));

			final ImmutableByteArray updatedKeystore;
			try (final SafePasswordHolder password = new SafePasswordHolder(
					sessionService.getCharArray(new Key(sessionId, component, Type.PASSWORD))
							.orElseThrow(() -> new NoSuchElementException("Missing password for keystore.")))) {

				updatedKeystore = KeystoreKeysImporter.importPublicKeys(keystore, password, component, componentKeys);
			}

			sessionService.putBytes(new Key(sessionId, component, Type.KEYSTORE),
					updatedKeystore);
		});

		// Import public keys into the keystores the foreign components
		Arrays.stream(Alias.values())
				.filter(component -> !selectedComponents.contains(component))
				.filter(Alias::hasPrivateKey)
				.forEach(component -> sessionService.putCharArray(new Key(sessionId, component, PUBLIC_KEY),
						pemConverterService.toPem(componentKeys.get(component)).toCharArray()));

		LOGGER.info("Imported public keys successfully. [sessionId: {}]", sessionId);

		sessionService.setPhase(sessionId, Phase.KEYSTORES_DOWNLOAD);
	}

	/**
	 * Extracts the fingerprints for the direct trust certificates.
	 *
	 * @return a map with the alias as keys and fingerprints as values.
	 */
	public ImmutableMap<String, String> extractFingerprints(final String sessionId) {
		validateSessionId(sessionId);
		checkState(sessionService.getSessionPhase(sessionId).equals(Phase.KEYSTORES_DOWNLOAD));

		final ImmutableMap<String, String> fingerprints = Arrays.stream(Alias.values())
				.map(alias -> {
					if (alias.hasPrivateKey()) {
						return ImmutableMap.entry(alias.get(), extractFingerprintForComponent(sessionId, alias));
					} else {
						return ImmutableMap.entry(alias.get(), "-");
					}
				})
				.collect(toImmutableMap());

		LOGGER.info("Fingerprints extracted successfully. [sessionId: {}]", sessionId);

		return fingerprints;
	}

	private String extractFingerprintForComponent(final String sessionId, final Alias component) {
		final ImmutableByteArray certificateBytes = sessionService.getBytes(
						new SessionService.Key(sessionId, component, PUBLIC_KEY))
				.orElseThrow(() -> new NoSuchElementException("Missing public key."));
		final Certificate certificate = pemConverterService.fromPem(new String(certificateBytes.elements(), StandardCharsets.UTF_8));

		return CertificateHashCalculator.calculateFingerprintForCertificate(certificate);
	}
}
