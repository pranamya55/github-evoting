/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;

@Entity
@Table(name = "VERIFICATION_CARD")
public class VerificationCardEntity {

	@Id
	private String verificationCardId;

	@ManyToOne
	@JoinColumn(name = "VERIFICATION_CARD_SET_FK_ID", referencedColumnName = "VERIFICATION_CARD_SET_ID")
	private VerificationCardSetEntity verificationCardSetEntity;

	private String credentialId;

	private String votingCardId;

	@Convert(converter = VoterAuthenticationDataConverter.class)
	private SetupComponentVoterAuthenticationData voterAuthenticationData;

	@PrimaryKeyJoinColumn
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	private VerificationCardStateEntity verificationCardStateEntity;

	@Version
	private Integer changeControlId;

	public VerificationCardEntity() {
	}

	public VerificationCardEntity(final String verificationCardId, final VerificationCardSetEntity verificationCardSetEntity,
			final String credentialId, final String votingCardId, final SetupComponentVoterAuthenticationData voterAuthenticationData,
			final VerificationCardStateEntity verificationCardStateEntity) {
		this.verificationCardId = validateUUID(verificationCardId);
		this.verificationCardSetEntity = checkNotNull(verificationCardSetEntity);
		this.credentialId = validateUUID(credentialId);
		this.votingCardId = validateUUID(votingCardId);
		this.voterAuthenticationData = checkNotNull(voterAuthenticationData);
		this.verificationCardStateEntity = checkNotNull(verificationCardStateEntity);
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public VerificationCardSetEntity getVerificationCardSetEntity() {
		return verificationCardSetEntity;
	}

	public String getCredentialId() {
		return credentialId;
	}

	public String getVotingCardId() {
		return votingCardId;
	}

	public SetupComponentVoterAuthenticationData getVoterAuthenticationData() {
		return voterAuthenticationData;
	}

	public VerificationCardStateEntity getVerificationCardStateEntity() {
		return verificationCardStateEntity;
	}

}
