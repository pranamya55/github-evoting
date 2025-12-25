/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;

/**
 * Regroups the inputs needed by the CreateLVCCShare algorithm.
 *
 * <ul>
 *     <li>CK<sub>id</sub>, the confirmation key. Not null.</li>
 *     <li>k'<sub>j</sub>, the CCRj Return Codes Generation secret key. Not null.</li>
 * </ul>
 */
public record CreateLVCCShareInput(GqElement confirmationKey, ZqElement ccrjReturnCodesGenerationSecretKey) {

	public CreateLVCCShareInput {
		checkNotNull(confirmationKey);
		checkNotNull(ccrjReturnCodesGenerationSecretKey);

		checkArgument(confirmationKey.getGroup().hasSameOrderAs(ccrjReturnCodesGenerationSecretKey.getGroup()),
				"The confirmation key must have the same order as the CCRj Return Codes Generation secret key.");
	}

}
