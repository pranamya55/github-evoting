/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.validations.PasswordValidation;

/**
 * Regroups the inputs needed by the SetupTallyEB algorithm.
 * <ul>
 *     <li>(EL<sub>pk,1</sub>, EL<sub>pk,2</sub>, EL<sub>pk,3</sub>, EL<sub>pk,4</sub>), the CCM election public keys. Not null.</li>
 *     <li>(&pi;<sub>ELpk,1</sub>, &pi;<sub>ELpk,2</sub>, &pi;<sub>ELpk,3</sub>, &pi;<sub>ELpk,4</sub>), the CCM Schnorr proofs of knowledge. Not null.</li>
 *     <li>(PW<sub>0</sub>, ... , PW<sub>k-1</sub>), the passwords of k electoral board members. Not null.</li>
 * </ul>
 */
public record SetupTallyEBInput(GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys,
								GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> ccmSchnorrProofs,
								ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords) {

	/**
	 * Constructor for a SetupTallyEBInput.
	 *
	 * @param ccmElectionPublicKeys          (EL<sub>pk,1</sub>, EL<sub>pk,2</sub>, EL<sub>pk,3</sub>, EL<sub>pk,4</sub>), the CCM election public
	 *                                       keys as a {@link GroupVector}. Non-null.
	 * @param ccmSchnorrProofs               π<sub>ELpk</sub>, the schnorr proofs as a {@link GroupVector}. Non-null.
	 * @param electoralBoardMembersPasswords (PW<sub>0</sub>, ... , PW<sub>k-1</sub>), the passwords of k electoral board members. Non-null. Each
	 *                                       password must be at least 12 characters long, must contain at least one upper case letter, one lower case
	 *                                       letter, one special character and one digit.
	 * @throws NullPointerException     if any of the input parameters is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>there are not exactly 4 Control Component public keys.</li>
	 *                                      <li>the Control Component public keys do not contain the expected node ids.</li>
	 *                                      <li>the Control Component public keys and Schnorr proofs do not have the same size.</li>
	 *                                      <li>k is strictly smaller than 2.</li>
	 *                                      <li>the passwords do not fulfill the policy.</li>
	 *                                  </ul>
	 */
	public SetupTallyEBInput {
		checkNotNull(ccmElectionPublicKeys);
		checkNotNull(ccmSchnorrProofs);
		checkArgument(ccmElectionPublicKeys.getGroup().hasSameOrderAs(ccmSchnorrProofs.getGroup()),
				"The CCM election public keys and the Schnorr proofs must have the same group order.");
		checkArgument(ccmElectionPublicKeys.size() == ccmSchnorrProofs.size(),
				"There must be as many CCM Schnorr proofs as CCM election public keys.");
		checkArgument(ccmElectionPublicKeys.size() == ControlComponentNode.ids().size(), "There must be exactly %s CCM election public keys.",
				ControlComponentNode.ids().size());

		checkArgument(ccmElectionPublicKeys.getElementSize() == ccmSchnorrProofs.getElementSize(),
				"The size of the CCM election public keys must be equal to the size of the CCM Schnorr proofs.");

		checkNotNull(electoralBoardMembersPasswords).forEach(pwd -> PasswordValidation.validate(pwd.get(), "electoral board member"));
		checkArgument(electoralBoardMembersPasswords.size() >= 2, "There must be at least 2 electoral board members.");
	}
}
