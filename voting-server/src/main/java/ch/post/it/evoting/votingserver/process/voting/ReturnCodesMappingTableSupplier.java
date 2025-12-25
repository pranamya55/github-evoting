/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.ReturnCodesMappingTableService;

/**
 * Provides a {@link ReturnCodesMappingTable} associated to a particular verification card set. The Return Codes Mapping Table is backed by the
 * database.
 */
@Service
public class ReturnCodesMappingTableSupplier {

	private final ReturnCodesMappingTableService returnCodesMappingTableService;

	public ReturnCodesMappingTableSupplier(final ReturnCodesMappingTableService returnCodesMappingTableService) {
		this.returnCodesMappingTableService = returnCodesMappingTableService;
	}

	/**
	 * Constructs and gets a {@link ReturnCodesMappingTable} associated to the given {@code verificationCardSetId}.
	 *
	 * @param verificationCardSetId the verification card set id for which to get the Return Codes Mapping Table.
	 * @return the Return Codes Mapping Table interface for this {@code verificationCardSetId}.
	 * @throws NullPointerException      if {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code verificationCardSetId} is an invalid UUID.
	 */
	public ReturnCodesMappingTable get(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		return hashedLongReturnCode -> {
			checkNotNull(hashedLongReturnCode);

			return returnCodesMappingTableService.getEncryptedShortReturnCode(verificationCardSetId, hashedLongReturnCode);
		};
	}

}
