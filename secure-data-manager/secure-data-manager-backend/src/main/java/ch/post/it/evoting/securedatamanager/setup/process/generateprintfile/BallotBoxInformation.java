/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the information of a ballot box.
 */
public record BallotBoxInformation(String name, String verificationCardSetId, boolean isTest, int countICH, int countACH, int countForeigner) {

	/**
	 * Constructs the Ballot Box information.
	 *
	 * @param name                  the name of the ballot box. Must be non-null.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @param isTest                whether the ballot box is a test ballot box.
	 * @param countICH              the count of swiss resident voters. Must be greater or equal to 0.
	 * @param countACH              the count of swiss abroad voters. Must be greater or equal to 0.
	 * @param countForeigner        the count of foreigner voters. Must be greater or equal to 0.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code verificationCardSetId} is not a valid UUID.
	 * @throws IllegalArgumentException  if {@code countICH}, {@code countACH} or {@code countForeigner} is negative.
	 */
	public BallotBoxInformation {
		checkNotNull(name);
		validateUUID(verificationCardSetId);
		checkArgument(countICH >= 0, "The countICH must be greater or equal to 0. [countICH: %s]", countICH);
		checkArgument(countACH >= 0, "The countACH must be greater or equal to 0. [countACH: %s]", countACH);
		checkArgument(countForeigner >= 0, "The countForeigner must be greater or equal to 0. [countForeigner: %s]", countForeigner);
	}
}
