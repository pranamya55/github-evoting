/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@Service
public class CreateLVCCShareService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateLVCCShareService.class);

	private final CreateLVCCShareAlgorithm createLVCCShareAlgorithm;
	private final CcrjReturnCodesKeysService ccrjReturnCodesKeysService;

	@Value("${nodeID}")
	private int nodeId;

	public CreateLVCCShareService(final CreateLVCCShareAlgorithm createLVCCShareAlgorithm,
			final CcrjReturnCodesKeysService ccrjReturnCodesKeysService) {
		this.createLVCCShareAlgorithm = createLVCCShareAlgorithm;
		this.ccrjReturnCodesKeysService = ccrjReturnCodesKeysService;
	}

	/**
	 * Invokes the CreateLVCCShare algorithm.
	 *
	 * @param encryptionGroup the encryption group. Must be non-null.
	 * @param confirmationKey the confirmation key. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the group of the confirmation key is not equal to the encryption group.
	 */
	public CreateLVCCShareOutput createLVCCShare(final GqGroup encryptionGroup, final ConfirmationKey confirmationKey) {
		checkNotNull(encryptionGroup);
		checkNotNull(confirmationKey);
		checkArgument(confirmationKey.element().getGroup().equals(encryptionGroup));

		final ContextIds contextIds = confirmationKey.contextIds();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final ZqElement ccrjReturnCodesGenerationSecretKey = ccrjReturnCodesKeysService.getCcrjReturnCodesGenerationSecretKey(electionEventId);

		final LVCCHashContext context = new LVCCHashContext(encryptionGroup, nodeId, electionEventId, verificationCardSetId, verificationCardId);
		final CreateLVCCShareInput input = new CreateLVCCShareInput(confirmationKey.element(), ccrjReturnCodesGenerationSecretKey);

		LOGGER.debug("Performing CreateLVCCShare algorithm... [contextIds: {}, nodeId: {}]", contextIds, nodeId);

		return createLVCCShareAlgorithm.createLVCCShare(context, input);
	}
}
