/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally;

public enum BallotBoxStatus {
	READY(1),
	MIXING_NOT_STARTED(2),
	MIXING(3),
	MIXED(4),
	MIXING_ERROR(5),
	DOWNLOADED(6),
	DECRYPTING(7),
	DECRYPTED(8);

	private final int index;

	BallotBoxStatus(final int index) {
		this.index = index;
	}

	/**
	 * Returns whether this status is after a given one.
	 *
	 * @param other the other status
	 * @return is after.
	 */
	public boolean isAfter(final BallotBoxStatus other) {
		return index > other.index;
	}
}
