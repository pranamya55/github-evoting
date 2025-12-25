/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.PartialChoiceReturnCodeAllowList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

@DisplayName("Construct CreateLCCShareInput with")
class CreateLCCShareInputTest extends TestGroupSetup {

	private static final Hash hash = spy(HashFactory.createHash());
	private static final Base64 base64 = BaseEncodingFactory.createBase64();

	private PartialChoiceReturnCodeAllowList pCCAllowList;
	private GroupVector<GqElement, GqGroup> partialChoiceReturnCodes;
	private ZqElement ccrjReturnCodesGenerationSecretKey;

	@BeforeEach
	@SuppressWarnings("java:S117")
	void setup() {
		final SecureRandom secureRandom = new SecureRandom();
		final int psi = secureRandom.nextInt(1, 5);

		// pCCAllowList
		final GqGroup gqGroup = GroupTestData.getLargeGqGroup();
		final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(gqGroup);
		boolean allDistinct;
		do {
			partialChoiceReturnCodes = gqGroupGenerator.genRandomGqElementVector(psi);
			allDistinct = partialChoiceReturnCodes.stream()
					.allMatch(ConcurrentHashMap.newKeySet()::add);
		}
		while (!allDistinct);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final ImmutableList<String> allowList = partialChoiceReturnCodes.stream()
				.map(pCC_id_i -> hash.hashAndSquare(pCC_id_i.getValue(), gqGroup))
				.map(hpCC_id_i -> hash.recursiveHash(hpCC_id_i, HashableString.from(uuidGenerator.generate()),
						HashableString.from(uuidGenerator.generate())))
				.map(base64::base64Encode)
				.collect(toImmutableList());
		pCCAllowList = allowList::contains;

		ccrjReturnCodesGenerationSecretKey = zqGroupGenerator.genRandomZqElementMember();
	}

	@Test
	@DisplayName("any null parameters throws NullPointerException")
	void constructWithNullParametersThrows() {
		assertThrows(NullPointerException.class, () -> new CreateLCCShareInput(null, partialChoiceReturnCodes, ccrjReturnCodesGenerationSecretKey));
		assertThrows(NullPointerException.class, () -> new CreateLCCShareInput(pCCAllowList, null, ccrjReturnCodesGenerationSecretKey));
		assertThrows(NullPointerException.class, () -> new CreateLCCShareInput(pCCAllowList, partialChoiceReturnCodes, null));
	}

	@Test
	@DisplayName("partial choice return codes different group order than CCRj return codes generation secret key throws IllegalArgumentException")
	void constructWithGroupsOfDifferentOrderThrows() {
		final ZqElement ccrjReturnCodesGenerationSecretKeyDifferentGroup = otherZqGroupGenerator.genRandomZqElementMember();
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new CreateLCCShareInput(pCCAllowList, partialChoiceReturnCodes, ccrjReturnCodesGenerationSecretKeyDifferentGroup));
		assertEquals("The partial choice return codes and return codes generation secret key must have the same group order.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("wrong number of partial choice return codes throws IllegalArgumentException")
	void wrongNumberOfLongVoteCastReturnCodeSharesThrows() {
		// Too few.
		final GroupVector<GqElement, GqGroup> emptyPartialChoiceReturnCodes = GroupVector.of();
		assertThrows(IllegalArgumentException.class,
				() -> new CreateLCCShareInput(pCCAllowList, emptyPartialChoiceReturnCodes, ccrjReturnCodesGenerationSecretKey));

		// Too many.
		final GroupVector<GqElement, GqGroup> tooManyPartialChoiceReturnCodes = gqGroupGenerator.genRandomGqElementVector(
				MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1);
		assertThrows(IllegalArgumentException.class,
				() -> new CreateLCCShareInput(pCCAllowList, tooManyPartialChoiceReturnCodes, ccrjReturnCodesGenerationSecretKey));
	}

}
