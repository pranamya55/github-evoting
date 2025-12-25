/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.precompute;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.PRE_COMPUTE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;
import ch.post.it.evoting.securedatamanager.shared.workflow.PreWorkflowTask;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

@Service
@ConditionalOnProperty("role.isSetup")
public class PreComputeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreComputeService.class);

	private final WorkflowStepRunner workflowStepRunner;
	private final VerificationCardSetService verificationCardSetService;
	private final FixedBaseOptimizationsService fixedBaseOptimizationsService;
	private final VerificationCardSetPreComputeService verificationCardSetPreComputeService;

	public PreComputeService(
			final WorkflowStepRunner workflowStepRunner,
			final VerificationCardSetService verificationCardSetService,
			final FixedBaseOptimizationsService fixedBaseOptimizationsService,
			final VerificationCardSetPreComputeService verificationCardSetPreComputeService) {
		this.workflowStepRunner = workflowStepRunner;
		this.verificationCardSetService = verificationCardSetService;
		this.fixedBaseOptimizationsService = fixedBaseOptimizationsService;
		this.verificationCardSetPreComputeService = verificationCardSetPreComputeService;
	}

	/**
	 * Implements the pre-compute process:
	 * <ul>
	 *     <li>update the ballots,</li>
	 *     <li>limit the number of values in the cache,</li>
	 *     <li>pre-compute the verification card sets.</li>
	 * </ul>
	 *
	 * @param electionEventId the election event id. Must be non-null and valid.
	 */
	public void preCompute(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Pre-computing the election event... [electionEventId:{}]", electionEventId);

		// Preparation work done before the actual pre-computation.
		final PreWorkflowTask<Void> preWorkflowTask = new PreWorkflowTask<>(() -> {
			performPreWorkflowTask(electionEventId);
			return null;
		});

		// Verification card sets pre-computation.
		// Retrieve all verification card sets not yet pre-computed.
		final ImmutableList<VerificationCardSetEntity> verificationCardSets = verificationCardSetService.getVerificationCardSets(Status.READY);

		final ImmutableList<WorkflowTask> workflowTasks = verificationCardSets.stream()
				.map(verificationCardSet ->
						new WorkflowTask(
								() -> performPreCompute(electionEventId, verificationCardSet.getVerificationCardSetId(),
										verificationCardSet.getBallotBoxEntity().getBallotBoxId()),
								() -> LOGGER.info("Pre-compute of verification card set successful. [verificationCardSetId: {}]",
										verificationCardSet.getVerificationCardSetId()),
								throwable -> LOGGER.error("Pre-compute of verification card set failed. [verificationCardSetId: {}]",
										verificationCardSet.getVerificationCardSetId(), throwable)
						)
				)
				.collect(toImmutableList());

		workflowStepRunner.run(PRE_COMPUTE, preWorkflowTask, workflowTasks);
	}

	private void performPreWorkflowTask(final String electionEventId) {
		// Limit the number of small primes and public keys saved in the cache.
		fixedBaseOptimizationsService.prepareFixedBaseOptimizations(electionEventId);
	}

	private void performPreCompute(final String electionEventId, final String verificationCardSetId, final String ballotBoxId) {
		// Construct the matching verification card set context.
		final PrecomputeContext precomputeContext = new PrecomputeContext(electionEventId, ballotBoxId, verificationCardSetId);

		verificationCardSetPreComputeService.preComputeVerificationCardSet(precomputeContext);
	}

}
