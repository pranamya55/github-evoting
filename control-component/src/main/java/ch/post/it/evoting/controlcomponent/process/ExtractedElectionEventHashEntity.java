/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "EXTRACTED_ELECTION_EVENT_HASH")
public class ExtractedElectionEventHashEntity {

	@Id
	private String electionEventId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ELECTION_EVENT_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	private String extractedElectionEventHash;

	@Version
	@Column(name = "CHANGE_CONTROL_ID")
	private Integer changeControlId;

	public ExtractedElectionEventHashEntity() {
	}

	public ExtractedElectionEventHashEntity(final ElectionEventEntity electionEventEntity, final String extractedElectionEventHash) {
		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.extractedElectionEventHash = validateBase64Encoded(extractedElectionEventHash);

		checkArgument(extractedElectionEventHash.length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
				String.format("The length of the extracted election event hash must be equal to l_HB64. [l_HB64: %s]",
						BASE64_ENCODED_HASH_OUTPUT_LENGTH));
	}

	public String getExtractedElectionEventHash() {
		return extractedElectionEventHash;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ExtractedElectionEventHashEntity that = (ExtractedElectionEventHashEntity) o;
		return Objects.equals(electionEventId, that.electionEventId) && Objects.equals(electionEventEntity, that.electionEventEntity)
				&& Objects.equals(extractedElectionEventHash, that.extractedElectionEventHash) && Objects.equals(changeControlId,
				that.changeControlId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, electionEventEntity, extractedElectionEventHash, changeControlId);
	}
}
