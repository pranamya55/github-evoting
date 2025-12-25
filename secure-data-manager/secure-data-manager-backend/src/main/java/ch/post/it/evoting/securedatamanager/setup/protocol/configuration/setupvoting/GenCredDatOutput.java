/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the output needed by the GenCredDat algorithm.
 *
 * <ul>
 *     <li>VCks, the vector of verification card keystores. Not null.</li>
 * </ul>
 */
public record GenCredDatOutput(ImmutableList<String> verificationCardKeystores) {

	public GenCredDatOutput {
		checkNotNull(verificationCardKeystores);
		checkArgument(!verificationCardKeystores.isEmpty(), "The vector of verification card keystores must not be empty.");
		verificationCardKeystores.forEach(vcKeyStore -> {
			Validations.validateBase64Encoded(vcKeyStore);
			VerificationCardKeystore.validateKeystoreSize(vcKeyStore);
		});
	}
}
