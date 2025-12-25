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
public interface ReturnCodesMappingTableRepository extends CrudRepository<ReturnCodesMappingTableEntryEntity, String> {

	@Query("select e.encryptedShortReturnCode from ReturnCodesMappingTableEntryEntity e where e.verificationCardSetEntity.verificationCardSetId = ?1 and e.hashedLongReturnCode = ?2")
	Optional<String> findByHashedLongReturnCode(final String verificationCardSetId, final String hashLongReturnCode);

}
