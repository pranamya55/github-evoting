/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface BallotBoxRepository extends CrudRepository<BallotBoxEntity, String> {

	@Query("select e from BallotBoxEntity e where e.verificationCardSetEntity.verificationCardSetId = ?1")
	Optional<BallotBoxEntity> findByVerificationCardSetId(final String verificationCardSetId);

}
