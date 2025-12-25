/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.converters.EncryptionGroupConverter;

@Entity
@Table(name = "ELECTION_EVENT")
public class ElectionEventEntity {

	@Id
	@Column(name = "ELECTION_EVENT_ID")
	private String electionEventId;

	@Convert(converter = EncryptionGroupConverter.class)
	private GqGroup encryptionGroup;

	@Version
	private Integer changeControlId;

	public ElectionEventEntity() {
	}

	public ElectionEventEntity(final String electionEventId, final GqGroup encryptionGroup) {
		this.electionEventId = validateUUID(electionEventId);
		this.encryptionGroup = checkNotNull(encryptionGroup);
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}
}
