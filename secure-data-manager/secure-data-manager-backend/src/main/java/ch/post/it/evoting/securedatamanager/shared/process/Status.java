/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

/**
 * Defines a set of status for entities.
 */
public enum Status {
	READY(1),
	PRECOMPUTING(2), // Still needed?
	PRECOMPUTED(3),

	COMPUTING(4),
	COMPUTED(5),
	VCS_DOWNLOADED(6),
	GENERATED(7),
	CONSTITUTED(8),
	UPLOADED(9);

	private final int index;

	Status(final int index) {
		this.index = index;
	}

	/**
	 * Returns whether this status is after a given one.
	 *
	 * @param other the other status
	 * @return is after.
	 */
	public boolean isAfter(final Status other) {
		return index > other.index;
	}
}
