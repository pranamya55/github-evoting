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
public interface EncryptedVerifiableVoteRepository extends CrudRepository<EncryptedVerifiableVoteEntity, String> {

	@Query("select e from EncryptedVerifiableVoteEntity e "
			+ "where e.verificationCardEntity.verificationCardSetEntity.verificationCardSetId = ?1 "
			+ "and e.verificationCardEntity.verificationCardStateEntity.confirmed = true")
	List<EncryptedVerifiableVoteEntity> findAllConfirmedByVerificationCardSetId(final String verificationCardSetId);

	@Query("select e from EncryptedVerifiableVoteEntity e "
			+ "where e.verificationCardEntity.verificationCardSetEntity.electionEventEntity.electionEventId = ?1 "
			+ "and e.verificationCardEntity.verificationCardStateEntity.lccShareCreated = true")
	List<EncryptedVerifiableVoteEntity> findAllSentByElectionEventId(final String electionEventId);

}
