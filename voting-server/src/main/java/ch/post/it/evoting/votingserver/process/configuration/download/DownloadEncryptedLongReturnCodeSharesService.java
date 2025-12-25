/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.download;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.votingserver.process.configuration.EncLongCodeShareEntity;
import ch.post.it.evoting.votingserver.process.configuration.EncLongCodeShareRepository;

@Service
public class DownloadEncryptedLongReturnCodeSharesService {

	private final EncLongCodeShareRepository encLongCodeShareRepository;

	public DownloadEncryptedLongReturnCodeSharesService(final EncLongCodeShareRepository encLongCodeShareRepository) {
		this.encLongCodeShareRepository = encLongCodeShareRepository;
	}

	public ImmutableList<ImmutableByteArray> download(final String electionEventId, final String verificationCardSetId, final int chunkId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkId >= 0);

		final ImmutableList<EncLongCodeShareEntity> encLongCodeShareEntities = ImmutableList.from(
				encLongCodeShareRepository.findByVerificationCardSetIdAndChunkId(verificationCardSetId, chunkId));

		final int numberOfEncLongCodeShares = encLongCodeShareEntities.size();
		final int numberOfControlComponents = ControlComponentNode.ids().size();

		if (numberOfEncLongCodeShares != numberOfControlComponents) {
			throw new IllegalStateException(String.format(
					"The number of enc long code shares doesn't match the number of nodes. [numberOfEncLongCodeShares: %s, numberOfControlComponents: %s]",
					numberOfEncLongCodeShares, numberOfControlComponents));
		}

		return encLongCodeShareEntities.stream()
				.map(EncLongCodeShareEntity::getEncLongCodeShare)
				.collect(toImmutableList());
	}
}
