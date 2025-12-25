/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;

class GenCredDatOutputTest {

	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final Random random = RandomFactory.createRandom();
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final int SIZE = secureRandom.nextInt(12) + 1;

	private final ImmutableList<String> verificationCardKeystores = Stream.generate(
					() -> "%s=".formatted(random.genRandomString(571, base64Alphabet)))
			.limit(SIZE)
			.collect(toImmutableList());

	@Test
	@DisplayName("happyPath")
	void happyPath() {
		final AtomicReference<GenCredDatOutput> refOutput = new AtomicReference<>();

		assertDoesNotThrow(() -> refOutput.set(new GenCredDatOutput(verificationCardKeystores)));

		final GenCredDatOutput output = refOutput.get();

		assertTrue(output.verificationCardKeystores().containsAll(verificationCardKeystores),
				"Input verificationCardKeystores does not contains all elements");
	}

	@Test
	@DisplayName("argument null")
	void nullArgumentTest() {
		final NullPointerException ex =
				assertThrows(NullPointerException.class, () -> new GenCredDatOutput(null));

		final String expectedMessage = null;

		assertEquals(expectedMessage, ex.getMessage());
	}

	@Test
	@DisplayName("argument empty")
	void emptyArgumentTest() {
		final ImmutableList<String> emptyList = ImmutableList.emptyList();

		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new GenCredDatOutput(emptyList));

		final String expectedMessage = "The vector of verification card keystores must not be empty.";

		assertEquals(expectedMessage, ex.getMessage());
	}
}
