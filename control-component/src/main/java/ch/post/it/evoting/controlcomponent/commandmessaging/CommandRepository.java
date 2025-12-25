/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.commandmessaging;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface CommandRepository extends CrudRepository<CommandEntity, CommandId> {

	List<CommandEntity> findAllByContextIdAndContextAndNodeId(String contextId, String context, Integer nodeId);

	List<CommandEntity> findAllByCorrelationId(String correlationId);
}
