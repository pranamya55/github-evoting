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
public interface PCCAllowListEntryRepository extends CrudRepository<PCCAllowListEntryEntity, String> {

	@Query("select e from PCCAllowListEntryEntity e where e.verificationCardSetEntity.verificationCardSetId = ?1")
	List<PCCAllowListEntryEntity> findAllByVerificationCardSetId(final String verificationCardSetId);

	@Query("select case when count(e.partialChoiceReturnCode) = 1 then true else false end from PCCAllowListEntryEntity e where e.verificationCardSetEntity.verificationCardSetId = ?1 and e.partialChoiceReturnCode = ?2")
	boolean existsByPartialChoiceReturnCode(final String verificationCardSetId, final String partialChoiceReturnCode);
}
