/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;

import java.time.Instant;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base16Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;

@Service
public class WorkflowLogService {
	private final WorkflowLogRepository workflowLogRepository;

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base16Alphabet = Base16Alphabet.getInstance();

	public WorkflowLogService(
			final WorkflowLogRepository workflowLogRepository) {
		this.workflowLogRepository = workflowLogRepository;
	}

	public synchronized void saveLog(final WorkflowStep workflowStep, final WorkflowStatus workflowStatus, final String contextId,
			final WorkflowExceptionCode exceptionCode) {
		final WorkflowLog log = new WorkflowLog(random.genRandomString(ID_LENGTH, base16Alphabet), workflowStep.name(), workflowStatus.name(),
				contextId, exceptionCode.name(), Instant.now());
		workflowLogRepository.save(log);
	}

	public synchronized ImmutableList<WorkflowLog> findLogs() {
		return ImmutableList.from(workflowLogRepository.findAll());
	}
}
