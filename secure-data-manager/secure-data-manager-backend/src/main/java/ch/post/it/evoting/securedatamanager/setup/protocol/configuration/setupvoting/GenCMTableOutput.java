/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_VOTE_CAST_RETURN_CODE_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;

/**
 * Regroups the outputs produced by the GenCMTable algorithm.
 *
 * <ul>
 *    <li>CMtable, the Return Codes Mapping table. Non-null.</li>
 *    <li>(CC<sub>0</sub>, ..., CC<sub>N_E-1</sub>), the vector of short Choice Return Codes. Non-null.</li>
 *    <li>(VCC<sub>0</sub>, ..., VCC<sub>N_E-1</sub>), the vector of short Vote Cast Return Codes. Non-null.</li>
 * </ul>
 */
@SuppressWarnings("java:S115")
public record GenCMTableOutput(ImmutableMap<String, String> returnCodesMappingTable, ImmutableList<ImmutableList<String>> shortChoiceReturnCodes,
							   ImmutableList<String> shortVoteCastReturnCodes) {

	private static final int l_HB64 = BASE64_ENCODED_HASH_OUTPUT_LENGTH;

	/**
	 * @throws NullPointerException     if any of the fields is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>The size of {@code returnCodesMappingTable} is zero.</li>
	 *                                      <li>The size of {@code shortChoiceReturnCodes} is zero.</li>
	 *                                      <li>The size of any {@code shortChoiceReturnCodes} element is zero.</li>
	 *                                      <li>The size of {@code shortVoteCastReturnCodesCopy} is zero.</li>
	 *                                      <li>The Return Codes Mapping table key length is not {@value GenCMTableOutput#l_HB64}.</li>
	 *                                      <li>The short Choice Return Codes elements length is not {@value ch.post.it.evoting.evotinglibraries.domain.common.Constants#SHORT_CHOICE_RETURN_CODE_LENGTH}.</li>
	 *                                      <li>The short Vote Cast Return elements length is not {@value ch.post.it.evoting.evotinglibraries.domain.common.Constants#SHORT_CHOICE_RETURN_CODE_LENGTH}.</li>
	 *                                      <li>The number of elements of {@code shortChoiceReturnCodes} and {@code shortVoteCastReturnCodes} are not equal.</li>
	 *                                      <li>The {@code returnCodesMappingTable} size is not equal to N_E * (n + 1).</li>
	 *                                  </ul>
	 */
	public GenCMTableOutput {
		checkNotNull(returnCodesMappingTable);

		// Not empty lists check
		checkArgument(!returnCodesMappingTable.isEmpty(), "Return Codes Mapping table must not be empty.");
		checkArgument(!shortChoiceReturnCodes.isEmpty(), "Short Choice Return Codes must not be empty.");
		checkArgument(shortChoiceReturnCodes.stream().map(ImmutableList::size).allMatch(size -> size > 0),
				"Short Choice Return Codes must not contain empty lists.");
		checkArgument(!shortVoteCastReturnCodes.isEmpty(), "Vote Cast Return Codes must not be empty.");

		// Values length check
		checkArgument(returnCodesMappingTable.keySet().stream().parallel()
						.allMatch(key -> validateBase64Encoded(key).length() == l_HB64),
				String.format("The CM table's keys must be valid Base64 string of length %s.", l_HB64));
		checkArgument(returnCodesMappingTable.values().stream().parallel()
						.allMatch(value -> validateBase64Encoded(value).length() == GenCMTableAlgorithm.ENCODED_CHOICE_RETURN_CODE_LENGTH
								|| value.length() == GenCMTableAlgorithm.ENCODED_CAST_RETURN_CODE_LENGTH),
				String.format("The CM table's values must be valid Base64 string of length %s or %s.",
						GenCMTableAlgorithm.ENCODED_CHOICE_RETURN_CODE_LENGTH, GenCMTableAlgorithm.ENCODED_CAST_RETURN_CODE_LENGTH));
		checkArgument(
				shortChoiceReturnCodes.stream().flatMap(
						ImmutableList::stream).allMatch(cc -> cc.length() == SHORT_CHOICE_RETURN_CODE_LENGTH),
				String.format("Short Choice Return Codes values must have a length of %s.", SHORT_CHOICE_RETURN_CODE_LENGTH));
		checkArgument(shortVoteCastReturnCodes.stream().allMatch(vcc -> vcc.length() == SHORT_VOTE_CAST_RETURN_CODE_LENGTH),
				String.format("Short Vote Cast Return Codes values must have a length of %s.", SHORT_VOTE_CAST_RETURN_CODE_LENGTH));

		// Elements size checks.
		checkArgument(shortChoiceReturnCodes.size() == shortVoteCastReturnCodes.size(),
				"Short Choice Return Codes and short Vote Cast Return Codes must have the same number of elements.");

		// Expected CMtable size : N_E * (n + 1)
		final int expectedReturnCodesMappingTableSize = shortChoiceReturnCodes.size() * (shortChoiceReturnCodes.get(0).size() + 1);
		checkArgument(returnCodesMappingTable.size() == expectedReturnCodesMappingTableSize,
				String.format("Return Codes Mapping table must have a size of %s.", expectedReturnCodesMappingTableSize));
	}

}
