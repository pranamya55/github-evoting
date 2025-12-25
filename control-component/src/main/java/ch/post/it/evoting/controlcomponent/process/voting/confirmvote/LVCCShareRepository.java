/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface LVCCShareRepository extends CrudRepository<LVCCShareEntity, LVCCShareEntityKey> {

	List<LVCCShareEntity> findAllByVerificationCardIdAndConfirmationKey(final String verificationCardId, final String confirmationKey);

	boolean existsByVerificationCardIdAndConfirmationKey(final String verificationCardId, final String confirmationKey);

}
