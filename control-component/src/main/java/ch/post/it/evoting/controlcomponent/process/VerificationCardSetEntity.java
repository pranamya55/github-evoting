/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.converters.ImmutableListConverter;

@Entity
@Table(name = "VERIFICATION_CARD_SET")
public class VerificationCardSetEntity {

	@Id
	@Column(name = "VERIFICATION_CARD_SET_ID")
	private String verificationCardSetId;

	@ManyToOne
	@JoinColumn(name = "ELECTION_EVENT_FK_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	private String verificationCardSetAlias;

	private String verificationCardSetDescription;

	@Convert(converter = ImmutableListConverter.class)
	private ImmutableList<String> domainsOfInfluence;

	@Version
	private Integer changeControlId;

	public VerificationCardSetEntity() {
	}

	private VerificationCardSetEntity(final String verificationCardSetId, final String verificationCardSetAlias,
			final String verificationCardSetDescription, final ImmutableList<String> domainsOfInfluence,
			final ElectionEventEntity electionEventEntity) {

		this.verificationCardSetId = validateUUID(verificationCardSetId);
		this.verificationCardSetAlias = validateXsToken(verificationCardSetAlias);
		this.verificationCardSetDescription = validateNonBlankUCS(verificationCardSetDescription);
		this.domainsOfInfluence = checkNotNull(domainsOfInfluence);
		this.electionEventEntity = checkNotNull(electionEventEntity);

		checkArgument(!domainsOfInfluence.isEmpty(), "The list of domains of influence cannot be empty.");
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public String getVerificationCardSetAlias() {
		return verificationCardSetAlias;
	}

	public String getVerificationCardSetDescription() {
		return verificationCardSetDescription;
	}

	public ElectionEventEntity getElectionEventEntity() {
		return electionEventEntity;
	}

	public ImmutableList<String> getDomainsOfInfluence() {
		return domainsOfInfluence;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final VerificationCardSetEntity that = (VerificationCardSetEntity) o;
		return Objects.equals(verificationCardSetId, that.verificationCardSetId) && Objects.equals(electionEventEntity,
				that.electionEventEntity) && Objects.equals(verificationCardSetAlias, that.verificationCardSetAlias)
				&& Objects.equals(verificationCardSetDescription, that.verificationCardSetDescription) && Objects.equals(
				domainsOfInfluence, that.domainsOfInfluence) && Objects.equals(changeControlId, that.changeControlId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(verificationCardSetId, electionEventEntity, verificationCardSetAlias, verificationCardSetDescription, domainsOfInfluence,
				changeControlId);
	}

	public static class Builder {

		private String verificationCardSetId;
		private String verificationCardSetAlias;
		private String verificationCardSetDescription;
		private ImmutableList<String> domainsOfInfluence;
		private ElectionEventEntity electionEventEntity;

		public Builder() {
			// Do nothing
		}

		public Builder setVerificationCardSetId(final String verificationCardSetId) {
			this.verificationCardSetId = verificationCardSetId;
			return this;
		}

		public Builder setVerificationCardSetAlias(final String verificationCardSetAlias) {
			this.verificationCardSetAlias = verificationCardSetAlias;
			return this;
		}

		public Builder setVerificationCardSetDescription(final String verificationCardSetDescription) {
			this.verificationCardSetDescription = verificationCardSetDescription;
			return this;
		}

		public Builder setElectionEventEntity(final ElectionEventEntity electionEventEntity) {
			this.electionEventEntity = electionEventEntity;
			return this;
		}

		public Builder setDomainsOfInfluence(final ImmutableList<String> domainsOfInfluence) {
			this.domainsOfInfluence = domainsOfInfluence;
			return this;
		}

		public VerificationCardSetEntity build() {
			return new VerificationCardSetEntity(verificationCardSetId, verificationCardSetAlias, verificationCardSetDescription, domainsOfInfluence,
					electionEventEntity);
		}
	}
}
