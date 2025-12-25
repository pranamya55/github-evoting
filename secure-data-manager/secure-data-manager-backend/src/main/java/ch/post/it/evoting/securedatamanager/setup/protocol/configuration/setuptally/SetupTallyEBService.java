/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally;

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
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
@ConditionalOnProperty("role.isSetup")
public class SetupTallyEBService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupTallyEBService.class);

	private final SetupTallyEBAlgorithm setupTallyEBAlgorithm;

	public SetupTallyEBService(final SetupTallyEBAlgorithm setupTallyEBAlgorithm) {
		this.setupTallyEBAlgorithm = setupTallyEBAlgorithm;
	}

	/**
	 * Invokes the SetupTallyEB algorithm.
	 *
	 * @param electionEventContextPayload    the election event context payload. Must be non-null.
	 * @param electoralBoardMembersPasswords the passwords of the electoral board members. Must be non-null and a valid EBPassword.
	 * @param controlComponentPublicKeys     the control component public keys. Must be non-null.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 * @throws IllegalArgumentException  if
	 *                                   <ul>
	 *                                    <li>no election event exists with the given election event id.</li>
	 *                                    <li>there is a wrong number of control component public keys.</li>
	 *                                   </ul>
	 */
	public SetupTallyEBOutput setupTallyEB(final ElectionEventContextPayload electionEventContextPayload,
			final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords,
			final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys) {
		checkNotNull(electionEventContextPayload);
		checkNotNull(electoralBoardMembersPasswords);
		checkArgument(electoralBoardMembersPasswords.size() >= 2, "There must be at least two passwords.");

		checkNotNull(controlComponentPublicKeys);
		checkArgument(controlComponentPublicKeys.size() == ControlComponentNode.ids().size(),
				"Wrong number of control component public keys. [expected: %s, actual: %s]", ControlComponentNode.ids(),
				controlComponentPublicKeys.size());

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys = controlComponentPublicKeys.stream()
				.map(ControlComponentPublicKeys::ccmjElectionPublicKey)
				.collect(GroupVector.toGroupVector());
		final GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> ccmSchnorrProofs = controlComponentPublicKeys.stream()
				.map(ControlComponentPublicKeys::ccmjSchnorrProofs)
				.collect(GroupVector.toGroupVector());

		final SetupTallyEBInput setupTallyEBInput = new SetupTallyEBInput(ccmElectionPublicKeys, ccmSchnorrProofs, electoralBoardMembersPasswords);

		final String electionEventId = electionEventContext.electionEventId();
		LOGGER.debug("Performing Setup Tally EB algorithm... [electionEventId: {}]", electionEventId);

		return setupTallyEBAlgorithm.setupTallyEB(electionEventContext, setupTallyEBInput);
	}
}
