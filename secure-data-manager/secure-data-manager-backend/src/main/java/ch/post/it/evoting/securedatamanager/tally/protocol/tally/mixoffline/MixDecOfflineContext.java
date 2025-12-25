/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the context values needed by MixDecOffline algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Non-null.</li>
 *     <li>ee, the election event id. A valid UUID.</li>
 *     <li>bb, the ballot box id. A valid UUID.</li>
 *     <li>&delta;, the number of allowed write-ins + 1. In range [1, &delta;<sub>sup</sub>].</li>
 * </ul>
 */
public class MixDecOfflineContext {

	private final GqGroup encryptionGroup;
	private final String electionEventId;
	private final String ballotBoxId;
	private final int numberOfAllowedWriteInsPlusOne;

	private MixDecOfflineContext(final GqGroup encryptionGroup, final String electionEventId, final String ballotBoxId,
			final int numberOfAllowedWriteInsPlusOne) {
		this.encryptionGroup = encryptionGroup;
		this.electionEventId = electionEventId;
		this.ballotBoxId = ballotBoxId;
		this.numberOfAllowedWriteInsPlusOne = numberOfAllowedWriteInsPlusOne;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public int getNumberOfAllowedWriteInsPlusOne() {
		return numberOfAllowedWriteInsPlusOne;
	}

	public static class Builder {

		private GqGroup encryptionGroup;
		private String electionEventId;
		private String ballotBoxId;
		private int numberOfAllowedWriteInsPlusOne;

		public Builder setEncryptionGroup(final GqGroup encryptionGroup) {
			this.encryptionGroup = encryptionGroup;
			return this;
		}

		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		public Builder setBallotBoxId(final String ballotBoxId) {
			this.ballotBoxId = ballotBoxId;
			return this;
		}

		public Builder setNumberOfAllowedWriteInsPlusOne(final int numberOfAllowedWriteInsPlusOne) {
			this.numberOfAllowedWriteInsPlusOne = numberOfAllowedWriteInsPlusOne;
			return this;
		}

		/**
		 * Creates a MixDecOfflineContext object.
		 *
		 * @throws NullPointerException      if the encryption group, the election event id or the ballot box id is null.
		 * @throws FailedValidationException if the election event id or the ballot box id is not a valid UUID.
		 * @throws IllegalArgumentException  if the number of allowed write-ins + 1 is not in the range [1, &delta;<sub>sup</sub>].
		 */
		public MixDecOfflineContext build() {
			checkNotNull(encryptionGroup);
			validateUUID(electionEventId);
			validateUUID(ballotBoxId);
			checkArgument(numberOfAllowedWriteInsPlusOne >= 1,
					"The number of allowed write-ins + 1 must be greater than or equal to 1. [delta: %s]",
					numberOfAllowedWriteInsPlusOne);
			checkArgument(numberOfAllowedWriteInsPlusOne <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
					"The number of write-ins plus one must be smaller or equal to the maximum supported number of write-ins plus one. [delta: %s, delta_sup: %s]",
					numberOfAllowedWriteInsPlusOne, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

			return new MixDecOfflineContext(encryptionGroup, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne);
		}
	}
}
