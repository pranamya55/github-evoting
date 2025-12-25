/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.LVCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCardSet;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;

/**
 * Implements the ExtractVerificationCardSet algorithm.
 */
@Service
public class ExtractVerificationCardSetAlgorithm {

	private final GetHashContextAlgorithm getHashContextAlgorithm;
	private final LVCCAllowListEntryService lvccAllowListEntryService;
	private final PCCAllowListEntryService pccAllowListEntryService;

	public ExtractVerificationCardSetAlgorithm(
			final GetHashContextAlgorithm getHashContextAlgorithm,
			final LVCCAllowListEntryService lvccAllowListEntryService,
			final PCCAllowListEntryService pccAllowListEntryService) {
		this.getHashContextAlgorithm = getHashContextAlgorithm;
		this.lvccAllowListEntryService = lvccAllowListEntryService;
		this.pccAllowListEntryService = pccAllowListEntryService;
	}

	/**
	 * Extracts the verification card set for the given context.
	 *
	 * @param context the {@link ExtractVerificationCardSetContext}. Must be non-null.
	 * @return the extracted verification card set as a {@link ExtractedVerificationCardSet}.
	 * @throws NullPointerException if the context is null.
	 */
	@SuppressWarnings("java:S117")
	ExtractedVerificationCardSet extractVerificationCardSet(final ExtractVerificationCardSetContext context) {
		checkNotNull(context);

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final String ee = context.electionEventId();
		final String vcs = context.verificationCardSetId();
		final PrimesMappingTable pTable = context.primesMappingTable();
		final ElGamalMultiRecipientPublicKey EL_pk = context.electionPublicKey();
		final ElGamalMultiRecipientPublicKey pk_CCR = context.choiceReturnCodesEncryptionPublicKey();

		// Operation.
		final String h = getHashContextAlgorithm.getHashContext(p_q_g, ee, vcs, pTable, EL_pk, pk_CCR);

		final ImmutableList<String> L_pCC = pccAllowListEntryService.getPartialChoiceReturnCodes(vcs);

		final ImmutableList<String> L_lVCC = lvccAllowListEntryService.getLongVoteCastReturnCodes(vcs);

		// Output.
		return new ExtractedVerificationCardSet(h, vcs, L_pCC, L_lVCC);
	}

}
