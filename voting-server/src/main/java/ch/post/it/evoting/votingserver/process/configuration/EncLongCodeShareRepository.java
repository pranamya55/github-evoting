/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface EncLongCodeShareRepository extends CrudRepository<EncLongCodeShareEntity, EncLongCodeSharePrimaryKey> {

	long countByVerificationCardSetId(final String verificationCardSetId);

	List<EncLongCodeShareEntity> findByVerificationCardSetIdAndChunkId(final String verificationCardSetId, int chunkId);

}
