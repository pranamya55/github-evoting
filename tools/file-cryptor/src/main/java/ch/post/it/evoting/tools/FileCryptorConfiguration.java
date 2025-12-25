/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.StreamableSymmetricEncryptionDecryptionService;

@Configuration
public class FileCryptorConfiguration {

	@Bean
	Random random() {
		return RandomFactory.createRandom();
	}

	@Bean
	Argon2 argon2() {
		return Argon2Factory.createArgon2(Argon2Profile.STANDARD);
	}

	@Bean
	StreamableSymmetricEncryptionDecryptionService streamableSymmetricEncryptionDecryptionService(final Random random, final Argon2 argon2) {
		return new StreamableSymmetricEncryptionDecryptionService(random, argon2);
	}

}
