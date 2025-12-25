/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend;

public enum ResetMode {
	ALWAYS_ENABLED,
	ENABLED_AT_END,
	NEVER_ENABLED;

	public static ResetMode getDefaultResetMode() {
		return ENABLED_AT_END;
	}
}
