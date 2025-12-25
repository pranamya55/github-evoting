/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "ELECTION_EVENT")
public class ElectionEventEntity {

	@Id
	@Column(name = "ELECTION_EVENT_ID")
	private String electionEventId;

	@Column(name = "DEFAULT_TITLE")
	private String defaultTitle;

	@Column(name = "DEFAULT_DESCRIPTION")
	private String defaultDescription;

	@Column(name = "ALIAS")
	private String alias;

	@Column(name = "DATE_FROM")
	private LocalDateTime dateFrom;

	@Column(name = "DATE_TO")
	private LocalDateTime dateTo;

	@Column(name = "GRACE_PERIOD")
	private int gracePeriod;

	@Version
	private int changeControlId;

	public ElectionEventEntity() {
	}

	public ElectionEventEntity(final String electionEventId, final String defaultTitle, final String defaultDescription, final String alias,
			final LocalDateTime dateFrom, final LocalDateTime dateTo, final int gracePeriod) {
		this.electionEventId = validateUUID(electionEventId);
		this.defaultTitle = checkNotNull(defaultTitle);
		this.defaultDescription = checkNotNull(defaultDescription);
		this.alias = checkNotNull(alias);
		this.dateFrom = checkNotNull(dateFrom);
		this.dateTo = checkNotNull(dateTo);
		this.gracePeriod = gracePeriod;
	}

	public String getElectionEventId() {
		return electionEventId;
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

	public LocalDateTime getDateFrom() {
		return dateFrom;
	}

	public LocalDateTime getDateTo() {
		return dateTo;
	}

	public int getGracePeriod() {
		return gracePeriod;
	}

}
