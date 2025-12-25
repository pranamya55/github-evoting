/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.BooleanConverter;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;
import ch.post.it.evoting.evotinglibraries.domain.validations.GracePeriodValidation;

@Entity
@Table(name = "BALLOT_BOX")
public class BallotBoxEntity {

	@Id
	@Column(name = "BALLOT_BOX_ID")
	private String ballotBoxId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_SET_FK_ID", referencedColumnName = "VERIFICATION_CARD_SET_ID")
	private VerificationCardSetEntity verificationCardSetEntity;


	private LocalDateTime ballotBoxStartTime;
	private LocalDateTime ballotBoxFinishTime;

	@Convert(converter = BooleanConverter.class)
	private boolean mixed = false;

	@Convert(converter = BooleanConverter.class)
	private boolean testBallotBox = false;

	// The grace period in seconds.
	private int gracePeriod;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray primesMappingTable;

	@Version
	private Integer changeControlId;

	public BallotBoxEntity() {
	}

	private BallotBoxEntity(final String ballotBoxId, final VerificationCardSetEntity verificationCardSetEntity,
			final LocalDateTime ballotBoxStartTime, final LocalDateTime ballotBoxFinishTime, final boolean testBallotBox,
			final int gracePeriod, final ImmutableByteArray primesMappingTable) {
		this.ballotBoxId = validateUUID(ballotBoxId);
		this.verificationCardSetEntity = checkNotNull(verificationCardSetEntity);

		this.ballotBoxStartTime = checkNotNull(ballotBoxStartTime);
		this.ballotBoxFinishTime = checkNotNull(ballotBoxFinishTime);
		checkArgument(ballotBoxStartTime.isBefore(ballotBoxFinishTime) || ballotBoxStartTime.equals(ballotBoxFinishTime),
				"The ballot box start time must not be after the ballot box finish time.");

		this.testBallotBox = testBallotBox;

		this.gracePeriod = GracePeriodValidation.validate(gracePeriod);

		this.primesMappingTable = checkNotNull(primesMappingTable);
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public VerificationCardSetEntity getVerificationCardSetEntity() {
		return verificationCardSetEntity;
	}

	public Integer getChangeControlId() {
		return changeControlId;
	}

	public LocalDateTime getBallotBoxStartTime() {
		return ballotBoxStartTime;
	}

	public LocalDateTime getBallotBoxFinishTime() {
		return ballotBoxFinishTime;
	}

	public boolean isMixed() {
		return mixed;
	}

	public void setMixed() {
		this.mixed = true;
	}

	public boolean isTestBallotBox() {
		return testBallotBox;
	}

	public int getGracePeriod() {
		return gracePeriod;
	}

	public ImmutableByteArray getPrimesMappingTable() {
		return primesMappingTable;
	}

	public static class Builder {

		private String ballotBoxId;
		private VerificationCardSetEntity verificationCardSetEntity;
		private LocalDateTime ballotBoxStartTime;
		private LocalDateTime ballotBoxFinishTime;
		private boolean testBallotBox;
		private int gracePeriod;
		private ImmutableByteArray primesMappingTable;

		public Builder setBallotBoxId(final String ballotBoxId) {
			this.ballotBoxId = ballotBoxId;
			return this;
		}

		public Builder setVerificationCardSetEntity(final VerificationCardSetEntity verificationCardSetEntity) {
			this.verificationCardSetEntity = verificationCardSetEntity;
			return this;
		}

		public Builder setBallotBoxStartTime(final LocalDateTime ballotBoxStartTime) {
			this.ballotBoxStartTime = ballotBoxStartTime;
			return this;
		}

		public Builder setBallotBoxFinishTime(final LocalDateTime ballotBoxFinishTime) {
			this.ballotBoxFinishTime = ballotBoxFinishTime;
			return this;
		}

		public Builder setTestBallotBox(final boolean testBallotBox) {
			this.testBallotBox = testBallotBox;
			return this;
		}

		public Builder setGracePeriod(final int gracePeriod) {
			this.gracePeriod = gracePeriod;
			return this;
		}

		public Builder setPrimesMappingTable(final ImmutableByteArray primesMappingTable) {
			this.primesMappingTable = primesMappingTable;
			return this;
		}

		public BallotBoxEntity build() {
			return new BallotBoxEntity(ballotBoxId, verificationCardSetEntity, ballotBoxStartTime, ballotBoxFinishTime, testBallotBox,
					gracePeriod, primesMappingTable);
		}

	}
}
