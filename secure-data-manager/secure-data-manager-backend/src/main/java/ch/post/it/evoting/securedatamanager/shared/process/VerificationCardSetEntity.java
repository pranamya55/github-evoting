/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "BALLOT_BOX_FK_ID", referencedColumnName = "BALLOT_BOX_ID")
	private BallotBoxEntity ballotBoxEntity;

	@Column(name = "DEFAULT_TITLE")
	private String defaultTitle;

	@Column(name = "DEFAULT_DESCRIPTION")
	private String defaultDescription;

	@Column(name = "ALIAS")
	private String alias;

	@Column(name = "NUMBER_OF_ELIGIBLE_VOTERS")
	private Integer numberOfEligibleVoters;

	@Column(name = "DOMAINS_OF_INFLUENCE")
	@Convert(converter = ImmutableListConverter.class)
	private ImmutableList<String> domainsOfInfluence;

	@Version
	private int changeControlId;

	public VerificationCardSetEntity() {
	}

	public VerificationCardSetEntity(final String verificationCardSetId, final ElectionEventEntity electionEventEntity,
			final BallotBoxEntity ballotBoxEntity, final String defaultTitle, final String defaultDescription, final String alias,
			final Integer numberOfEligibleVoters, final ImmutableList<String> domainsOfInfluence) {
		this.verificationCardSetId = verificationCardSetId;
		this.electionEventEntity = electionEventEntity;
		this.ballotBoxEntity = ballotBoxEntity;
		this.defaultTitle = defaultTitle;
		this.defaultDescription = defaultDescription;
		this.alias = alias;
		this.numberOfEligibleVoters = numberOfEligibleVoters;
		this.domainsOfInfluence = domainsOfInfluence;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public ElectionEventEntity getElectionEventEntity() {
		return electionEventEntity;
	}

	public BallotBoxEntity getBallotBoxEntity() {
		return ballotBoxEntity;
	}

	public void setBallotBoxEntity(final BallotBoxEntity ballotBoxEntity) {
		this.ballotBoxEntity = ballotBoxEntity;
	}

	public String getDefaultTitle() {
		return defaultTitle;
	}

	public String getDefaultDescription() {
		return defaultDescription;
	}

	public String getAlias() {
		return alias;
	}

	public Integer getNumberOfEligibleVoters() {
		return numberOfEligibleVoters;
	}

	public ImmutableList<String> getDomainsOfInfluence() {
		return domainsOfInfluence;
	}
}
