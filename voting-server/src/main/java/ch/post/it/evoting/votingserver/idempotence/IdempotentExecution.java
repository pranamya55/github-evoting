/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.idempotence;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.PayloadHashConverter;

@Entity
@IdClass(IdempotentExecutionId.class)
public class IdempotentExecution {

	@Id
	private String context;

	@Id
	private String executionKey;

	@Convert(converter = PayloadHashConverter.class)
	private ImmutableByteArray payloadHash;

	@Version
	private Long changeControlId;

	protected IdempotentExecution() {
	}

	public IdempotentExecution(final String context, final String executionKey, final ImmutableByteArray payloadHash) {
		this.context = checkNotNull(context);
		this.executionKey = checkNotNull(executionKey);
		this.payloadHash = payloadHash;
	}

	public String getContext() {
		return context;
	}

	public String getExecutionKey() {
		return executionKey;
	}

	public ImmutableByteArray getPayloadHash() {
		return payloadHash;
	}
}
