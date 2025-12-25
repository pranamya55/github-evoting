/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.idempotence;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class IdempotentExecutionId implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private static final int MAX_LENGTH = 255;

	private String executionKey;
	private String context;

	protected IdempotentExecutionId() {
	}

	public IdempotentExecutionId(final String context, final String executionKey) {
		this.context = checkNotNull(context);
		this.executionKey = checkNotNull(executionKey);
		checkArgument(context.length() <= MAX_LENGTH, "context exceeds the max defined size [maxLength: {}]", MAX_LENGTH);
		checkArgument(executionKey.length() <= MAX_LENGTH, "executionKey exceeds the max defined size [maxLength: {}]", MAX_LENGTH);
	}

	public String getExecutionKey() {
		return executionKey;
	}

	public String getContext() {
		return context;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final IdempotentExecutionId that = (IdempotentExecutionId) o;
		return Objects.equals(executionKey, that.executionKey) && Objects.equals(context, that.context);
	}

	@Override
	public int hashCode() {
		return Objects.hash(executionKey, context);
	}

	@Override
	public String toString() {
		return "IdempotentExecutionId{" +
				"executionKey='" + executionKey + '\'' +
				", context='" + context + '\'' +
				'}';
	}
}
