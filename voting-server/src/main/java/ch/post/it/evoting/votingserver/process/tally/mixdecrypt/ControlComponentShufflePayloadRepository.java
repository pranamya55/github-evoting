/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface ControlComponentShufflePayloadRepository extends CrudRepository<ControlComponentShufflePayloadEntity, String> {

	List<ControlComponentShufflePayloadEntity> findByElectionEventIdAndBallotBoxIdOrderByNodeId(final String electionEventId,
			final String ballotBoxId);

	Integer countByElectionEventIdAndBallotBoxId(final String electionEventId, final String ballotBoxId);

}
