/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import java.util.Optional;

/**
 * Interface representing the Return Codes Mapping Table used by the voting phase algorithms.
 */
public interface ReturnCodesMappingTable {

	/**
	 * Retrieves the encrypted Short Return Code corresponding to the given {@code hashedLongReturnCode}.
	 *
	 * @param hashedLongReturnCode the hashed Long Return Code being the key in the Return Codes Mapping Table.
	 * @return the corresponding encrypted Short Return Code.
	 */
	Optional<String> get(final String hashedLongReturnCode);

}
