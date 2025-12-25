/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "VOTER_PORTAL_CONFIGURATION")
public class VoterPortalConfiguration {

	@Id
	private String electionEventId;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray config;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray favicon;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray logo;

	@Version
	private Integer changeControlId;

	public VoterPortalConfiguration(final String electionEventId) {
		this.electionEventId = validateUUID(electionEventId);
	}

	public VoterPortalConfiguration() {
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public ImmutableByteArray getConfig() {
		return config;
	}

	public void setConfig(final ImmutableByteArray config) {
		this.config = checkNotNull(config);
	}

	public ImmutableByteArray getFavicon() {
		return favicon;
	}

	public void setFavicon(final ImmutableByteArray favicon) {
		this.favicon = checkNotNull(favicon);
	}

	public ImmutableByteArray getLogo() {
		return logo;
	}

	public void setLogo(final ImmutableByteArray logo) {
		this.logo = checkNotNull(logo);
	}
}
