/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface InProgressMessageRepository extends CrudRepository<InProgressMessage, InProgressMessageId> {

	Integer countByCorrelationIdAndResponsePayloadIsNotNull(final String correlationId);

	List<InProgressMessage> findAllByCorrelationIdAndResponsePayloadIsNotNull(final String correlationId);

	Optional<InProgressMessage> findFirstByRequestMessageTypeAndContextId(final String requestMessageType, final String contextId);

	@Transactional
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<InProgressMessage> findAllByCorrelationIdOrderByNodeId(final String correlationId);

	@Transactional
	void deleteAllByCorrelationId(final String correlationId);
}
