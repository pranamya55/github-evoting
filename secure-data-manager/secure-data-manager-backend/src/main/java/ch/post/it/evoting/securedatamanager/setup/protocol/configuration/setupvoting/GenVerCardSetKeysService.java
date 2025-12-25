/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.GroupVectorElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.utils.Validations;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;

@Service
@ConditionalOnProperty("role.isSetup")
public class GenVerCardSetKeysService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenVerCardSetKeysService.class);

	private final GenVerCardSetKeysAlgorithm genVerCardSetKeysAlgorithm;

	public GenVerCardSetKeysService(final GenVerCardSetKeysAlgorithm genVerCardSetKeysAlgorithm) {
		this.genVerCardSetKeysAlgorithm = genVerCardSetKeysAlgorithm;
	}

	/**
	 * Invokes the GenVerCardSetKeys algorithm.
	 *
	 * @param electionEventContextPayload the election event context payload. Must be non-null.
	 * @param controlComponentPublicKeys  the control component public keys. Must be non-null.
	 */
	public ElGamalMultiRecipientPublicKey genVerCardSetKeys(final ElectionEventContextPayload electionEventContextPayload,
			final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys) {
		checkNotNull(electionEventContextPayload);
		checkNotNull(controlComponentPublicKeys);
		checkArgument(controlComponentPublicKeys.size() == ControlComponentNode.ids().size(),
				"Wrong number of control component public keys. [expected: %s, actual: %s]", ControlComponentNode.ids(),
				controlComponentPublicKeys.size());

		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		checkArgument(encryptionGroup.equals(controlComponentPublicKeys.get(0).ccrjChoiceReturnCodesEncryptionPublicKey().getGroup()),
				"The encryption group of the control component public keys does not match the encryption group of the election event context payload.");

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final int maximumNumberOfSelections = electionEventContext.maximumNumberOfSelections();

		checkArgument(Validations.allEqual(
				controlComponentPublicKeys.stream().map(ControlComponentPublicKeys::ccrjChoiceReturnCodesEncryptionPublicKey),
				GroupVectorElement::getGroup), "All ccrjChoiceReturnCodesEncryptionPublicKey elements must belong to the same group.");

		checkArgument(Validations.allEqual(
				controlComponentPublicKeys.stream().map(ControlComponentPublicKeys::ccrjSchnorrProofs),
				GroupVectorElement::getGroup), "All ccrjSchnorrProofs elements must belong to the same group.");

		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccrjChoiceReturnCodesEncryptionPublicKey = controlComponentPublicKeys.stream()
				.map(ControlComponentPublicKeys::ccrjChoiceReturnCodesEncryptionPublicKey)
				.collect(GroupVector.toGroupVector());
		final GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> ccrjSchnorrProofs = controlComponentPublicKeys.stream()
				.map(ControlComponentPublicKeys::ccrjSchnorrProofs)
				.collect(GroupVector.toGroupVector());

		final String electionEventId = electionEventContext.electionEventId();
		final GenVerCardSetKeysContext context = new GenVerCardSetKeysContext(encryptionGroup, electionEventId, maximumNumberOfSelections);
		final GenVerCardSetKeysInput input = new GenVerCardSetKeysInput(ccrjChoiceReturnCodesEncryptionPublicKey, ccrjSchnorrProofs);

		LOGGER.debug("Performing GenVerCardSetKeys algorithm... [electionEventId: {}]", electionEventId);

		return genVerCardSetKeysAlgorithm.genVerCardSetKeys(context, input);
	}
}
