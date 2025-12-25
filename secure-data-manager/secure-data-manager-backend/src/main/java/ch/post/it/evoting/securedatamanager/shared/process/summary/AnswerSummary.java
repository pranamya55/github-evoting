/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateLanguageMap;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.Language;

public record AnswerSummary(int answerPosition, ImmutableMap<String, String> answerInfo) {

	public AnswerSummary {
		checkArgument(answerPosition >= 0, "The answer position must be positive.");
		validateLanguageMap(checkNotNull(answerInfo).entrySet().stream()
				.collect(ImmutableMap.toImmutableMap(entry -> Language.valueOfInsensitive(entry.key()), ImmutableMap.Entry::value)));
	}
}
