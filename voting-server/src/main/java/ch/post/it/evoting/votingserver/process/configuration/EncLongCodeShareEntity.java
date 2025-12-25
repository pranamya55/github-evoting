/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

@Entity
@IdClass(EncLongCodeSharePrimaryKey.class)
@Table(name = "ENC_LONG_CODE_SHARE")
@SuppressWarnings("java:S1068")
public class EncLongCodeShareEntity {

	@Id
	@Column(name = "VERIFICATION_CARD_SET_ID")
	private String verificationCardSetId;

	@Id
	@Column(name = "CHUNK_ID")
	private int chunkId;

	@Id
	@Column(name = "NODE_ID")
	private int nodeId;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray encLongCodeShare;

	@Version
	private Integer changeControlId;

	public EncLongCodeShareEntity() {

	}

	public EncLongCodeShareEntity(final String verificationCardSetId, final int chunkId, final int nodeId,
			final ImmutableByteArray encLongCodeShare) {
		this.verificationCardSetId = validateUUID(verificationCardSetId);
		checkState(chunkId >= 0);
		this.chunkId = chunkId;
		checkState(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		this.nodeId = nodeId;
		this.encLongCodeShare = checkNotNull(encLongCodeShare);
	}

	public ImmutableByteArray getEncLongCodeShare() {
		return encLongCodeShare;
	}
}
