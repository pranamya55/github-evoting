/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

public enum SupportedFileType {

	CONFIG(Alias.CANTON),
	PRINT(Alias.SDM_CONFIG);

	private final Alias signingAlias;

	SupportedFileType(final Alias signingAlias) {
		this.signingAlias = signingAlias;
	}

	public Alias getSigningAlias() {
		return signingAlias;
	}
}
