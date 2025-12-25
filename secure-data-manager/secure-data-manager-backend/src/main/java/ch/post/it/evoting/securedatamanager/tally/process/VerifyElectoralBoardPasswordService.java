/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.configuration.ElectoralBoardHashesPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.BoardPasswordHashService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardHashesPayloadService;

@Service
@ConditionalOnProperty("role.isTally")
public class VerifyElectoralBoardPasswordService {
	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyElectoralBoardPasswordService.class);

	private final BoardPasswordHashService boardPasswordHashService;
	private final ElectoralBoardHashesPayloadService electoralBoardHashesPayloadService;
	private final SignatureKeystore<Alias> signatureKeystoreService;

	public VerifyElectoralBoardPasswordService(
			final BoardPasswordHashService boardPasswordHashService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectoralBoardHashesPayloadService electoralBoardHashesPayloadService) {
		this.electoralBoardHashesPayloadService = electoralBoardHashesPayloadService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.boardPasswordHashService = boardPasswordHashService;
	}

	/**
	 * Verifies the given electoral board member's password.
	 *
	 * @param electionEventId              the identifier of the election. Must be non-null and a valid UUID.
	 * @param electoralBoardMemberIdx      the index of the electoral board member. Must be positive and strictly smaller than the number of stored
	 *                                     EBHashes.
	 * @param electoralBoardMemberPassword the password of the electoral board member. Must be non-null and a valid EBPassword.
	 * @return true if the given password matches the stored hash of the electoral board member, false otherwise.
	 * @throws NullPointerException             if any input is null.
	 * @throws FailedValidationException        if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalArgumentException         if the {@code electoralBoardMemberIdx} is negative or greater than the number of stored EBHashes.
	 * @throws InvalidPayloadSignatureException if the signature of the electoral board hashes payload is invalid.
	 * @throws IllegalStateException            if the signature verification of the electoral board hashes was not possible.
	 */
	public boolean verifyElectoralBoardMemberPassword(final String electionEventId, final int electoralBoardMemberIdx,
			final SafePasswordHolder electoralBoardMemberPassword) {
		validateUUID(electionEventId);
		checkArgument(electoralBoardMemberIdx >= 0);
		checkNotNull(electoralBoardMemberPassword);

		final ElectoralBoardHashesPayload electoralBoardHashesPayload = electoralBoardHashesPayloadService.load(electionEventId);

		final CryptoPrimitivesSignature signature = electoralBoardHashesPayload.getSignature();

		checkState(signature != null, "The signature of the electoral board hashes payload is null. [electionEventId: %s]",
				electionEventId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentElectoralBoardHashes(electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, electoralBoardHashesPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the electoral board hashes payload. [electionEventId: %s]", electionEventId));
		}
		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(
					ElectoralBoardHashesPayload.class, String.format("[electionEventId: %s]", electionEventId));
		}
		LOGGER.info("Validated signature of electoral board hashes payload. [electionEventId: {}]", electionEventId);

		final ImmutableList<ImmutableByteArray> electoralBoardMembersHashes = electoralBoardHashesPayload.getElectoralBoardHashes();
		checkArgument(electoralBoardMemberIdx < electoralBoardMembersHashes.size());
		final ImmutableByteArray electoralBoardMemberArgonHash = electoralBoardMembersHashes.get(electoralBoardMemberIdx);

		return boardPasswordHashService.verifyPassword(electoralBoardMemberPassword, electoralBoardMemberArgonHash);
	}
}
