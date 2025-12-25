/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

public enum VerifierExportType {
	CONTEXT("context"),
	TALLY("tally");

	private final String rootPath;

	VerifierExportType(final String rootPath) {
		this.rootPath = rootPath;
	}

	public String rootPath() {
		return rootPath;
	}
}
