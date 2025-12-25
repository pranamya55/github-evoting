/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "VERIFICATION_CARD_SET_STATE")
public class VerificationCardSetStateEntity {

	@Id
	@Column(name = "VERIFICATION_CARD_SET_STATE_ID")
	private String verificationCardSetStateId;

	@Column(name = "STATUS")
	private String status;

	public VerificationCardSetStateEntity() {
		// Needed by the repository.
	}

	public VerificationCardSetStateEntity(final String verificationCardSetStateId, final String status) {
		this.verificationCardSetStateId = verificationCardSetStateId;
		this.status = status;
	}

	public String getVerificationCardSetStateId() {
		return verificationCardSetStateId;
	}

	public void setVerificationCardSetStateId(final String verificationCardSetStateId) {
		this.verificationCardSetStateId = verificationCardSetStateId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}
}
