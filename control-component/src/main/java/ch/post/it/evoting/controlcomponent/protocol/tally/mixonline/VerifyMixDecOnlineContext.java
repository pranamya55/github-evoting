/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.Stream;

import com.google.common.collect.Streams;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * Represents the context values needed by the VerifyMixDecOnline algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Not null.</li>
 *     <li>j, the control component index. In range [2, 4].</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>bb, a ballot box id. Not null and valid a UUID.</li>
 *     <li>&delta;, the number of allowed write-ins + 1 for this specific ballot box. In range [1, &delta;<sub>sup</sub>].</li>
 *     <li>EL<sub>pk</sub>, the election public key. Not null.</li>
 *     <li>(EL<sub>pk,1</sub>,EL<sub>pk,2</sub>,EL<sub>pk,3</sub>,EL<sub>pk,4</sub>), the CCM election public keys. Not null.</li>
 *     <li>EB<sub>pk</sub>, the electoral board public key. Not null.</li>
 * </ul>
 */
public record VerifyMixDecOnlineContext(GqGroup encryptionGroup,
										int nodeId,
										String electionEventId,
										String ballotBoxId,
										int numberOfWriteInsPlusOne,
										ElGamalMultiRecipientPublicKey electionPublicKey,
										GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys,
										ElGamalMultiRecipientPublicKey electoralBoardPublicKey) {

	public VerifyMixDecOnlineContext {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(electionPublicKey);
		checkNotNull(ccmElectionPublicKeys);
		checkNotNull(electoralBoardPublicKey);

		checkArgument(ControlComponentNode.ids().contains(nodeId) && nodeId != 1, "The control component index must be in range [2, %s].",
				ControlComponentNode.ids().size());

		checkArgument(numberOfWriteInsPlusOne >= 1, "The number of allowed write-ins + 1 must be greater than or equal to 1.");
		checkArgument(numberOfWriteInsPlusOne <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
				"The number of write-ins + 1 must be smaller or equal to the maximum supported number of write-ins + 1. [delta: %s, delta_sup: %s]",
				numberOfWriteInsPlusOne, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

		// Cross size checks.
		checkArgument(electionPublicKey.size() == electoralBoardPublicKey.size(),
				"The election public key and the electoral board public key must have the same size.");
		checkArgument(electionPublicKey.size() == ccmElectionPublicKeys.getElementSize(),
				"The election public key and the CCM election public keys must have the same size.");
		checkArgument(ccmElectionPublicKeys.size() == ControlComponentNode.ids().size(), "There must be exactly %s CCM election public keys.",
				ControlComponentNode.ids().size(),
				ControlComponentNode.ids().size(), ccmElectionPublicKeys.size());
		checkArgument(electionPublicKey.size() >= numberOfWriteInsPlusOne,
				"The election public key must have at least delta elements. [delta_max: %s, delta: %s]", electionPublicKey.size(),
				numberOfWriteInsPlusOne);
		checkArgument(ccmElectionPublicKeys.getElementSize() <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
				"The CCM election public keys must be smaller than or equal to the maximum supported number of write-ins + 1. [delta_max: %s, delta_sup: %s]",
				ccmElectionPublicKeys.getElementSize(), MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

		// Cross-group checks.
		checkArgument(encryptionGroup.equals(electionPublicKey.getGroup()),
				"The election public key's group must be equal to the encryption group.");
		checkArgument(encryptionGroup.equals(ccmElectionPublicKeys.getGroup()),
				"The CCM election public keys' group must be equal to the encryption group.");
		checkArgument(encryptionGroup.equals(electoralBoardPublicKey.getGroup()),
				"The electoral board public key's group must be equal to the encryption group.");

		// Check election public key
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> publicKeys = Streams.concat(
						ccmElectionPublicKeys.stream(),
						Stream.of(electoralBoardPublicKey))
				.collect(GroupVector.toGroupVector());

		checkArgument(electionPublicKey.equals(ElGamalFactory.createElGamal().combinePublicKeys(publicKeys)),
				"Multiplication of the ccmElectionPublicKeys times the electoralBoardPublicKey must equal the electionPublicKey.");
	}
}
