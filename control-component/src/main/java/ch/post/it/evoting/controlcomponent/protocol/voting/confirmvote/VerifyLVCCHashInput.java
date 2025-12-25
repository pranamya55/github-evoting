/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.controlcomponent.process.LongVoteCastReturnCodesAllowList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the inputs needed by the VerifyLVCCHash algorithm.
 *
 * <ul>
 *     <li>L<sub>lVCC</sub>, the long Vote Case Return Codes allow list. Not null and not empty.</li>
 *     <li>hlVCC<sub>j,id</sub>, the CCRj's hashed long Vote Cast Return Code share. Not null.</li>
 *     <li>(hlVCC<sub>j&#770;_1,id</sub>, hlVCC<sub>j&#770;_2,id</sub>, hlVCC<sub>j&#770;_3,id</sub>), the other CCRj's hashed long Vote Cast Return Code shares. Not null.</li>
 * </ul>
 */
public class VerifyLVCCHashInput {

	private final LongVoteCastReturnCodesAllowList longVoteCastReturnCodesAllowList;
	private final String ccrjHashedLongVoteCastReturnCode;
	private final ImmutableList<String> otherCCRsHashedLongVoteCastReturnCodes;

	private VerifyLVCCHashInput(
			final LongVoteCastReturnCodesAllowList longVoteCastReturnCodesAllowList,
			final String ccrjHashedLongVoteCastReturnCode,
			final ImmutableList<String> otherCCRsHashedLongVoteCastReturnCodes) {
		this.longVoteCastReturnCodesAllowList = longVoteCastReturnCodesAllowList;
		this.ccrjHashedLongVoteCastReturnCode = ccrjHashedLongVoteCastReturnCode;
		this.otherCCRsHashedLongVoteCastReturnCodes = otherCCRsHashedLongVoteCastReturnCodes;
	}

	LongVoteCastReturnCodesAllowList getLongVoteCastReturnCodesAllowList() {
		return longVoteCastReturnCodesAllowList;
	}

	public String getCcrjHashedLongVoteCastReturnCode() {
		return ccrjHashedLongVoteCastReturnCode;
	}

	public ImmutableList<String> getOtherCCRsHashedLongVoteCastReturnCodes() {
		return otherCCRsHashedLongVoteCastReturnCodes;
	}

	public static class Builder {
		private LongVoteCastReturnCodesAllowList longVoteCastReturnCodesAllowList;
		private String ccrjHashedLongVoteCastReturnCode;
		private ImmutableList<String> otherCCRsHashedLongVoteCastReturnCodes;

		public Builder setLongVoteCastReturnCodesAllowList(final LongVoteCastReturnCodesAllowList longVoteCastReturnCodesAllowList) {
			this.longVoteCastReturnCodesAllowList = checkNotNull(longVoteCastReturnCodesAllowList);
			return this;
		}

		public Builder setCcrjHashedLongVoteCastReturnCode(final String ccrjHashedLongVoteCastReturnCode) {
			this.ccrjHashedLongVoteCastReturnCode = ccrjHashedLongVoteCastReturnCode;
			return this;
		}

		public Builder setOtherCCRsHashedLongVoteCastReturnCodes(final ImmutableList<String> otherCCRsHashedLongVoteCastReturnCodes) {
			this.otherCCRsHashedLongVoteCastReturnCodes = otherCCRsHashedLongVoteCastReturnCodes;
			return this;
		}

		public VerifyLVCCHashInput build() {
			checkNotNull(longVoteCastReturnCodesAllowList);
			checkNotNull(ccrjHashedLongVoteCastReturnCode);
			checkNotNull(otherCCRsHashedLongVoteCastReturnCodes);

			// Size checks.
			checkArgument(this.otherCCRsHashedLongVoteCastReturnCodes.size() == ControlComponentNode.ids().size() - 1,
					"The number of other CCRs hashed long Vote Cast Return Codes must be equal to the number of known node ids - 1.");
			final int l_HB64 = BASE64_ENCODED_HASH_OUTPUT_LENGTH;
			checkArgument(ccrjHashedLongVoteCastReturnCode.length() == l_HB64,
					"The length of the CCRj hashed long Vote Cast Return Code shares elements and "
							+ "the Long Vote Cast Return Codes allow list elements must be equal to l_HB64. [l_HB64: %s]", l_HB64);

			// Cross-size checks.
			checkArgument(otherCCRsHashedLongVoteCastReturnCodes.stream().map(String::length).distinct().count() == 1,
					"The length of all the other CCRj hashed long Vote Cast Return Code shares must be equal.");

			// Base64 checks.
			validateBase64Encoded(this.ccrjHashedLongVoteCastReturnCode);
			this.otherCCRsHashedLongVoteCastReturnCodes.forEach(Validations::validateBase64Encoded);

			return new VerifyLVCCHashInput(longVoteCastReturnCodesAllowList, ccrjHashedLongVoteCastReturnCode,
					otherCCRsHashedLongVoteCastReturnCodes);
		}
	}

}
