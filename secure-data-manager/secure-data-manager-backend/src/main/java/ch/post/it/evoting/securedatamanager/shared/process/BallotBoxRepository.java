/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface BallotBoxRepository extends CrudRepository<BallotBoxEntity, String> {

	BallotBoxEntity findByDescription(String description);

	@Query("SELECT b FROM BallotBoxEntity b WHERE b.electionEventEntity.electionEventId = :electionEventId")
	List<BallotBoxEntity> findByElectionEventId(@Param("electionEventId") String electionEventId);

	@Query("SELECT b.ballotBoxId FROM BallotBoxEntity b WHERE b.electionEventEntity.electionEventId = :electionEventId")
	List<String> findAllIdsByElectionEventId(@Param("electionEventId") String electionEventId);
}
