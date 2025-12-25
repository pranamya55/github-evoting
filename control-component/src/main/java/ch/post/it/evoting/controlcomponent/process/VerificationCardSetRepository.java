/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface VerificationCardSetRepository extends CrudRepository<VerificationCardSetEntity, String> {

	@Query("select e from VerificationCardSetEntity e where e.electionEventEntity.electionEventId = ?1")
	List<VerificationCardSetEntity> findAllByElectionEventId(final String electionEventId);

}
