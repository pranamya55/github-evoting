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
public interface VerificationCardSetStateRepository extends CrudRepository<VerificationCardSetStateEntity, String> {

	List<VerificationCardSetStateEntity> findAll();

	@Query("SELECT v.verificationCardSetStateId FROM VerificationCardSetStateEntity v WHERE v.status = :status")
	List<String> findAllIdsByStatus(@Param("status") String status);
}
