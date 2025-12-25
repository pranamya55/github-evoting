/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.utils.Validations;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;

/**
 * Encapsulates the context of the ExtractVerificationCardSet algorithm.
 *
 * @param encryptionGroup                      (p, q, g), the encryption group. Must be non-null.
 * @param electionEventId                      ee, the election event id. Must be non-null and a valid UUID.
 * @param verificationCardSetId                vcs, the verification card set id. Must be non-null and a valid UUID.
 * @param primesMappingTable                   pTable, the primes mapping table. Must be non-null.
 * @param electionPublicKey                    EL<sub>pk</sub>, the election public key. Must be non-null.
 * @param choiceReturnCodesEncryptionPublicKey pk<sub>CCR</sub>, the choice return codes encryption public key. Must be non-null.
 */
public record ExtractVerificationCardSetContext(GqGroup encryptionGroup,
												String electionEventId,
												String verificationCardSetId,
												PrimesMappingTable primesMappingTable,
												ElGamalMultiRecipientPublicKey electionPublicKey,
												ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey) {

	public ExtractVerificationCardSetContext {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(primesMappingTable);
		checkNotNull(electionPublicKey);
		checkNotNull(choiceReturnCodesEncryptionPublicKey);

		checkArgument(Validations.allEqual(Stream.of(encryptionGroup, electionPublicKey.getGroup(), choiceReturnCodesEncryptionPublicKey.getGroup(),
				primesMappingTable.getEncryptionGroup()), Function.identity()), "All parameters must have the same group.");

		checkArgument(electionPublicKey.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
				"The size of the election public key must be smaller or equal to the maximum supported number of write-ins + 1. [delta_max: %s, delta_sup: %s]",
				electionPublicKey.size(), MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

		checkArgument(choiceReturnCodesEncryptionPublicKey.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The size of the CCR encryption public key must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				choiceReturnCodesEncryptionPublicKey.size(), MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
	}
}
