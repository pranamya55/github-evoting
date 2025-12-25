/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;

public record ListUnionDescriptionInfo(String language, String listUnionDescription) {
	public ListUnionDescriptionInfo {
		validateNonBlankUCS(language);
		validateNonBlankUCS(listUnionDescription);
	}
}
