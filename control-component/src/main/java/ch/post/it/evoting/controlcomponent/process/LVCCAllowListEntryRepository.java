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
public interface LVCCAllowListEntryRepository extends CrudRepository<LVCCAllowListEntryEntity, String> {

	@Query("select e from LVCCAllowListEntryEntity e where e.verificationCardSetEntity.verificationCardSetId = ?1")
	List<LVCCAllowListEntryEntity> findAllByVerificationCardSetId(final String verificationCardSetId);

	@Query("select case when count(e.longVoteCastReturnCode) = 1 then true else false end from LVCCAllowListEntryEntity e where e.verificationCardSetEntity.verificationCardSetId = ?1 and e.longVoteCastReturnCode = ?2")
	boolean existsByLongVoteCastReturnCode(final String verificationCardSetId, final String longVoteCastReturnCode);
}
