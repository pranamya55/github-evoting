/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface MixnetInitialCiphertextsRepository extends CrudRepository<MixnetInitialCiphertextsEntity, String> {

	@Query("select e from MixnetInitialCiphertextsEntity e where e.electionEventEntity.electionEventId = ?1 and e.ballotBoxId = ?2")
	Optional<MixnetInitialCiphertextsEntity> findByElectionEventIdAndBallotBoxId(final String electionEventId, final String ballotBoxId);
}
