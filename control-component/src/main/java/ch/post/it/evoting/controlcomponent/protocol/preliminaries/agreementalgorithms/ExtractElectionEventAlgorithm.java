/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import java.util.Comparator;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCardSet;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;

/**
 * Implements the ExtractElectionEvent algorithm.
 */
@Service
public class ExtractElectionEventAlgorithm {

	private final ElectionEventContextService electionEventContextService;
	private final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;
	private final ExtractVerificationCardSetAlgorithm extractVerificationCardSetAlgorithm;

	public ExtractElectionEventAlgorithm(
			final ElectionEventContextService electionEventContextService,
			final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm,
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			final ExtractVerificationCardSetAlgorithm extractVerificationCardSetAlgorithm) {
		this.electionEventContextService = electionEventContextService;
		this.getHashElectionEventContextAlgorithm = getHashElectionEventContextAlgorithm;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
		this.extractVerificationCardSetAlgorithm = extractVerificationCardSetAlgorithm;
	}

	/**
	 * Extracts the election event for the given election event id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the extracted election event as a {@link ExtractedElectionEvent}.
	 * @throws IllegalArgumentException if {@code electionEventId} is not a valid UUID.
	 * @throws NullPointerException     if {@code electionEventId} is null.
	 */
	@SuppressWarnings("java:S117")
	ExtractedElectionEvent extractElectionEvent(final String electionEventId) {

		// Context.
		final String ee = validateUUID(electionEventId);

		// Operation.
		final ElectionEventContext electionEventContext = electionEventContextService.getElectionEventContext(ee);

		final String hContext = getHashElectionEventContextAlgorithm.getHashElectionEventContext(electionEventContext);

		final ElGamalMultiRecipientPublicKey EL_pk = setupComponentPublicKeysService.getElectionPublicKey(ee);

		final ElGamalMultiRecipientPublicKey pk_CCR = setupComponentPublicKeysService.getChoiceReturnCodesEncryptionPublicKey(ee);

		final GqGroup p_q_g = electionEventContext.encryptionGroup();
		final ImmutableList<ExtractedVerificationCardSet> evcs = electionEventContext.verificationCardSetContexts().stream()
				// for i in [0, N_bb)
				.map(verificationCardSetContext -> {
					final String vcs = verificationCardSetContext.getVerificationCardSetId();
					final PrimesMappingTable pTable = verificationCardSetContext.getPrimesMappingTable();
					final ExtractVerificationCardSetContext context = new ExtractVerificationCardSetContext(p_q_g, ee, vcs, pTable, EL_pk, pk_CCR);

					return extractVerificationCardSetAlgorithm.extractVerificationCardSet(context);
				})
				.sorted(Comparator.comparing(ExtractedVerificationCardSet::verificationCardSetId)) // evcs <- Order(evcs)
				.collect(toImmutableList());

		// Output.
		return new ExtractedElectionEvent(hContext, p_q_g, ee, evcs);
	}

}
