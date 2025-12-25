/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.Language;

public record DescriptionSummary(String language, String shortDescription, String longDescription) {

	public DescriptionSummary {
		Language.valueOfInsensitive(checkNotNull(language));
		validateNonBlankUCS(shortDescription);
		validateNonBlankUCS(longDescription);
	}
}
