/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.commandmessaging;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;
import ch.post.it.evoting.domain.converters.PayloadHashConverter;

@Entity
@IdClass(CommandId.class)
@Table(name = "command")
public class CommandEntity {

	@Id
	private String contextId;

	@Id
	private String context;

	@Id
	private String correlationId;

	@Id
	private Integer nodeId;

	@Convert(converter = PayloadHashConverter.class)
	private ImmutableByteArray requestPayloadHash;

	private Instant requestTimestamp;

	@Convert(converter = PayloadHashConverter.class)
	private ImmutableByteArray responsePayloadHash;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray responsePayload;

	private Instant responseTimestamp;

	@Version
	private Long changeControlId;

	private CommandEntity(final Builder builder) {
		this.contextId = builder.contextId;
		this.context = builder.context;
		this.correlationId = builder.correlationId;
		this.nodeId = builder.nodeId;
		this.requestPayloadHash = builder.requestPayloadHash;
		this.requestTimestamp = Objects.nonNull(builder.requestTimestamp) ? builder.requestTimestamp : Instant.now();
		this.responsePayloadHash = builder.responsePayloadHash;
		this.responsePayload = builder.responsePayload;
		this.responseTimestamp = builder.responseTimestamp;
	}

	protected CommandEntity() {
	}

	public String getContextId() {
		return contextId;
	}

	public String getContext() {
		return context;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public Integer getNodeId() {
		return nodeId;
	}

	public Instant getRequestTimestamp() {
		return requestTimestamp;
	}

	public ImmutableByteArray getRequestPayloadHash() {
		return requestPayloadHash;
	}

	public ImmutableByteArray getResponsePayloadHash() {
		return responsePayloadHash;
	}

	public void setResponsePayloadHash(final ImmutableByteArray responsePayloadHash) {
		this.responsePayloadHash = responsePayloadHash;
	}

	public ImmutableByteArray getResponsePayload() {
		return responsePayload;
	}

	public void setResponsePayload(final ImmutableByteArray responsePayload) {
		this.responsePayload = responsePayload;
	}

	public Instant getResponseTimestamp() {
		return responseTimestamp;
	}

	public void setResponseTimestamp(final Instant responseDateTime) {
		this.responseTimestamp = responseDateTime;
	}

	public Long getChangeControlId() {
		return changeControlId;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String contextId;
		private String context;
		private String correlationId;
		private Integer nodeId;
		private ImmutableByteArray requestPayloadHash;
		private Instant requestTimestamp;
		private ImmutableByteArray responsePayloadHash;
		private ImmutableByteArray responsePayload;
		private Instant responseTimestamp;

		private Builder() {
			// Do nothing
		}

		public Builder commandId(final CommandId value) {
			this.contextId = value.getContextId();
			this.context = value.getContext();
			this.correlationId = value.getCorrelationId();
			this.nodeId = value.getNodeId();
			return this;
		}

		public Builder requestPayloadHash(final ImmutableByteArray requestPayloadHash) {
			this.requestPayloadHash = requestPayloadHash;
			return this;
		}

		public Builder requestTimestamp(final Instant requestTimestamp) {
			this.requestTimestamp = requestTimestamp;
			return this;
		}

		public Builder responsePayloadHash(final ImmutableByteArray responsePayloadHash) {
			this.responsePayloadHash = responsePayloadHash;
			return this;
		}

		public Builder responsePayload(final ImmutableByteArray responsePayload) {
			this.responsePayload = responsePayload;
			return this;
		}

		public Builder responseTimestamp(final Instant responseTimestamp) {
			this.responseTimestamp = responseTimestamp;
			return this;
		}

		public CommandEntity build() {
			checkNotNull(contextId);
			checkNotNull(context);
			checkNotNull(correlationId);
			checkNotNull(nodeId);
			return new CommandEntity(this);
		}
	}
}
