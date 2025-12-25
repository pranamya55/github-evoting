/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.shelf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;

@Entity
@Table(name = "WORKFLOW_SHELF")
public class WorkflowShelfEntity {

	@Id
	private String id;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray shelfData;

	private LocalDateTime creationDate;

	protected WorkflowShelfEntity() {
	}

	public WorkflowShelfEntity(final String id, final ImmutableByteArray shelfData) {
		this.id = checkNotNull(id);
		this.shelfData = checkNotNull(shelfData);
		this.creationDate = LocalDateTimeUtils.now();
	}

	public String getId() {
		return id;
	}

	public ImmutableByteArray getShelfData() {
		return shelfData;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}
}
