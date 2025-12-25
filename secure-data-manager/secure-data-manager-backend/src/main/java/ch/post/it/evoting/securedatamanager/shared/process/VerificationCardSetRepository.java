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
public interface VerificationCardSetRepository extends CrudRepository<VerificationCardSetEntity, String> {

	@Query("SELECT v FROM VerificationCardSetEntity v WHERE v.electionEventEntity.electionEventId = :electionEventId")
	List<VerificationCardSetEntity> findByElectionEventId(@Param("electionEventId") String electionEventId);

	List<VerificationCardSetEntity> findAllByVerificationCardSetIdIn(List<String> verificationCardSetIds);

	@Query("SELECT v.verificationCardSetId FROM VerificationCardSetEntity v WHERE v.electionEventEntity.electionEventId = :electionEventId")
	List<String> findAllIdsByElectionEventId(@Param("electionEventId") String electionEventId);
}
