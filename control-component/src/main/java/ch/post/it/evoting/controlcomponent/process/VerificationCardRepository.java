/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface VerificationCardRepository extends CrudRepository<VerificationCardEntity, String> {

	boolean existsByVerificationCardIdIn(final Set<String> verificationCardId);

	@Query("select count(e) from VerificationCardEntity e where e.verificationCardSetEntity.electionEventEntity.electionEventId = ?1 and e.verificationCardSetEntity.verificationCardSetId = ?2")
	int countByElectionEventIdAndVerificationCardSetId(final String electionEventId, final String verificationCardSetId);

}
