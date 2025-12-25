/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.tally.TallyComponentVotesPayload;

@Service
@ConditionalOnProperty("role.isTally")
public class TallyComponentVotesService {

	private final TallyComponentVotesFileRepository tallyComponentVotesFileRepository;

	public TallyComponentVotesService(final TallyComponentVotesFileRepository tallyComponentVotesFileRepository) {
		this.tallyComponentVotesFileRepository = tallyComponentVotesFileRepository;
	}

	public void save(final TallyComponentVotesPayload payload) {
		checkNotNull(payload);

		final String electionEventId = payload.getElectionEventId();
		final String ballotBoxId = payload.getBallotBoxId();

		checkState(!tallyComponentVotesFileRepository.exists(electionEventId, ballotBoxId),
				"Requested tally component votes payload already exists. [electionEventId: %s, ballotBoxId: %s]",
				electionEventId, ballotBoxId);

		tallyComponentVotesFileRepository.save(payload);
	}

	public TallyComponentVotesPayload load(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		return tallyComponentVotesFileRepository.load(electionEventId, ballotBoxId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Requested tally component votes payload is not present. [electionEventId: %s, ballotBoxId: %s]",
								electionEventId, ballotBoxId)));
	}
}
