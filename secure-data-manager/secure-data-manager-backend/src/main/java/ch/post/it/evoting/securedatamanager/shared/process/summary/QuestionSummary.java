/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public record QuestionSummary(int questionPosition, String questionNumber, ImmutableList<DescriptionSummary> questionInfo,
							  ImmutableList<AnswerSummary> answers) {

	public QuestionSummary {
		checkArgument(questionPosition >= 0, "The questionPosition must be positive.");
		validateNonBlankUCS(questionNumber);
		checkNotNull(questionInfo);
		checkNotNull(answers);
	}
}
