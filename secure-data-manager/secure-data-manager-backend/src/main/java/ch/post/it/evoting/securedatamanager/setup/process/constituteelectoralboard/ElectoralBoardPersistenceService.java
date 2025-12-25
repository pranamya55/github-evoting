/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.domain.configuration.ElectoralBoardHashesPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardHashesPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentPublicKeysPayloadService;

@Service
@ConditionalOnProperty("role.isSetup")
public class ElectoralBoardPersistenceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralBoardPersistenceService.class);

	private final SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ElectoralBoardHashesPayloadService electoralBoardHashesPayloadService;

	public ElectoralBoardPersistenceService(
			final SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectoralBoardHashesPayloadService electoralBoardHashesPayloadService) {
		this.setupComponentPublicKeysPayloadService = setupComponentPublicKeysPayloadService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.electoralBoardHashesPayloadService = electoralBoardHashesPayloadService;
	}

	/**
	 * Persists the given inputs in the electoral board and election event context json files.
	 *
	 * @param electionEventId                      the election event id. Must be non-null and a valid UUID.
	 * @param controlComponentPublicKeys           the control component public keys. Must be non-null.
	 * @param choiceReturnCodesEncryptionPublicKey the choice return codes encryption public key. Must be non-null.
	 * @param electionPublicKey                    the election public key. Must be non-null.
	 * @param electoralBoardPublicKey              the electoral board public key. Must be non-null.
	 * @param electoralBoardSchnorrProofs          the electoral board schnorr proofs. Must be non-null.
	 * @param electoralBoardMembersHashes          the hashes of the electoral board members' passwords. Must be non-null.
	 * @throws FailedValidationException if the election event id is invalid.
	 * @throws NullPointerException      if any of the inputs is null.
	 * @throws IllegalStateException     if any hash is empty.
	 */
	public void persist(final String electionEventId, final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys,
			final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey, final ElGamalMultiRecipientPublicKey electionPublicKey,
			final ElGamalMultiRecipientPublicKey electoralBoardPublicKey, final GroupVector<SchnorrProof, ZqGroup> electoralBoardSchnorrProofs,
			final ImmutableList<ImmutableByteArray> electoralBoardMembersHashes) {

		validateUUID(electionEventId);
		checkNotNull(controlComponentPublicKeys);
		checkNotNull(choiceReturnCodesEncryptionPublicKey);
		checkNotNull(electionPublicKey);
		checkNotNull(electoralBoardPublicKey);
		checkNotNull(electoralBoardSchnorrProofs);
		checkNotNull(electoralBoardMembersHashes);
		electoralBoardMembersHashes.forEach(hash -> checkArgument(!hash.isEmpty()));

		final ElectoralBoardHashesPayload electoralBoardHashesPayload = createElectoralBoardHashesPayload(electionEventId,
				electoralBoardMembersHashes);
		electoralBoardHashesPayloadService.save(electoralBoardHashesPayload);

		LOGGER.info("Electoral board hashes payload successfully persisted. [electionEventId: {}]", electionEventId);

		final SetupComponentPublicKeys setupComponentPublicKeys = new SetupComponentPublicKeys(controlComponentPublicKeys, electoralBoardPublicKey,
				electoralBoardSchnorrProofs, electionPublicKey, choiceReturnCodesEncryptionPublicKey);
		final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload = createSetupComponentPublicKeysPayload(electionPublicKey.getGroup(),
				electionEventId, setupComponentPublicKeys);
		setupComponentPublicKeysPayloadService.save(setupComponentPublicKeysPayload);

		LOGGER.info("Election event context payload successfully persisted. [electionEventId: {}]", electionEventId);
	}

	private ElectoralBoardHashesPayload createElectoralBoardHashesPayload(final String electionEventId,
			final ImmutableList<ImmutableByteArray> immutableHashes) {
		final ElectoralBoardHashesPayload electoralBoardHashesPayload = new ElectoralBoardHashesPayload(electionEventId, immutableHashes);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentElectoralBoardHashes(electionEventId);

		final CryptoPrimitivesSignature electoralBoardHashesPayloadSignature = getPayloadSignature(electoralBoardHashesPayload,
				additionalContextData);
		electoralBoardHashesPayload.setSignature(electoralBoardHashesPayloadSignature);
		return electoralBoardHashesPayload;
	}

	private SetupComponentPublicKeysPayload createSetupComponentPublicKeysPayload(final GqGroup group, final String electionEventId,
			final SetupComponentPublicKeys setupComponentPublicKeys) {
		final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload = new SetupComponentPublicKeysPayload(group, electionEventId,
				setupComponentPublicKeys);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentPublicKeys(electionEventId);

		final CryptoPrimitivesSignature electionEventContextPayloadSignature = getPayloadSignature(setupComponentPublicKeysPayload,
				additionalContextData);
		setupComponentPublicKeysPayload.setSignature(electionEventContextPayloadSignature);

		return setupComponentPublicKeysPayload;
	}

	private CryptoPrimitivesSignature getPayloadSignature(final SignedPayload payload, final Hashable additionalContextData) {
		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);
			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Failed to generate payload signature. [%s, %s]", payload.getClass().getName(), additionalContextData));
		}

	}

}
