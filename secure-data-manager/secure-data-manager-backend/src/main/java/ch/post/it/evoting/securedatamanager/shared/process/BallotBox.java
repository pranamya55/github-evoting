/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.GracePeriodValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateDateFromDateTo;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

public record BallotBox(String id, String description, LocalDateTime startTime, LocalDateTime finishTime, boolean test, int gracePeriod, String status) {
	public BallotBox {
		validateUUID(id);
		validateNonBlankUCS(description);
		validateDateFromDateTo(startTime, finishTime);
		validate(gracePeriod);
		checkNotNull(status);
	}

	public static final class BallotBoxBuilder {
		private String id;
		private String description;
		private LocalDateTime startTime;
		private LocalDateTime finishTime;
		private boolean test;
		private int gracePeriod;
		private String status;

		public BallotBoxBuilder() {
			// Intentionally left empty.
		}

		public BallotBoxBuilder setId(String id) {
			this.id = id;
			return this;
		}

		public BallotBoxBuilder setDescription(String description) {
			this.description = description;
			return this;
		}

		public BallotBoxBuilder setStartTime(LocalDateTime startTime) {
			this.startTime = startTime;
			return this;
		}

		public BallotBoxBuilder setFinishTime(LocalDateTime finishTime) {
			this.finishTime = finishTime;
			return this;
		}

		public BallotBoxBuilder setTest(boolean test) {
			this.test = test;
			return this;
		}

		public BallotBoxBuilder setGracePeriod(int gracePeriod) {
			this.gracePeriod = gracePeriod;
			return this;
		}

		public BallotBoxBuilder setStatus(String status) {
			this.status = status;
			return this;
		}

		public BallotBox build() {
			return new BallotBox(this.id, this.description, this.startTime, this.finishTime, this.test, this.gracePeriod, this.status);
		}
	}
}
