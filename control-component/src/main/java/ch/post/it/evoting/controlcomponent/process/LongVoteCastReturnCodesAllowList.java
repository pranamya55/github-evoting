/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

public interface LongVoteCastReturnCodesAllowList {

	boolean exists(final String longVoteCastReturnCode);
}
