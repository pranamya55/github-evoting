/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.reactor;

import static com.google.common.base.Preconditions.checkNotNull;

public record Box<T>(T boxed) {
	public Box {
		checkNotNull(boxed);
	}
}
