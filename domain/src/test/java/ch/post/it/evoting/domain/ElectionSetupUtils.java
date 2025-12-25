/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base16Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.UsabilityBase32Alphabet;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;

public class ElectionSetupUtils {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base16Alphabet = Base16Alphabet.getInstance();

	public static ImmutableList<String> genBlankCorrectnessInformation(final int psi) {
		return IntStream.range(0, psi)
				.mapToObj(i -> random.genRandomString(ID_LENGTH, base16Alphabet))
				.collect(toImmutableList());
	}

	public static String genStartVotingKey() {
		final UsabilityBase32Alphabet usabilityBase32Alphabet = UsabilityBase32Alphabet.getInstance();
		final int alphabetSize = usabilityBase32Alphabet.size();
		return IntStream.range(0, Constants.SVK_LENGTH)
				.mapToObj(i -> random.genRandomInteger(alphabetSize))
				.map(usabilityBase32Alphabet::get)
				.collect(Collectors.joining());
	}
}
