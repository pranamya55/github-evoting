/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

package ch.post.it.evoting.domain.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@DisplayName("Test of PartiallyDecryptedEncryptedPCC")
class PartiallyDecryptedEncryptedPCCTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private final String electionEventId = uuidGenerator.generate();
	private final String verificationCardSetId = uuidGenerator.generate();
	private final String verificationCardId = uuidGenerator.generate();
	private final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
	private final Integer nodeId = 1;
	private GroupVector<GqElement, GqGroup> exponentiatedGamma;
	private GroupVector<ExponentiationProof, ZqGroup> exponentiationProofs;

	@BeforeEach
	void setUp() {
		final GqGroup gqGroup = GroupTestData.getGqGroup();
		final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(gqGroup);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(gqGroup));

		// Create payload.
		exponentiatedGamma = gqGroupGenerator.genRandomGqElementVector(2);
		exponentiationProofs = Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(2)
				.collect(GroupVector.toGroupVector());
	}

	@Test
	@DisplayName("Check null arguments")
	void nullArgs() {

		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new PartiallyDecryptedEncryptedPCC(null, nodeId, exponentiatedGamma, exponentiationProofs)),
				() -> assertThrows(NullPointerException.class,
						() -> new PartiallyDecryptedEncryptedPCC(contextIds, nodeId, null, exponentiationProofs)),
				() -> assertThrows(NullPointerException.class,
						() -> new PartiallyDecryptedEncryptedPCC(contextIds, nodeId, exponentiatedGamma, null)),
				() -> assertDoesNotThrow(
						() -> new PartiallyDecryptedEncryptedPCC(contextIds, nodeId, exponentiatedGamma, exponentiationProofs))
		);
	}

	@Test
	@DisplayName("Check nodeIds")
	void nodeIdArgs() {

		assertAll(
				() -> assertThrows(IllegalArgumentException.class,
						() -> new PartiallyDecryptedEncryptedPCC(contextIds, 0, exponentiatedGamma, exponentiationProofs)),
				() -> assertDoesNotThrow(() -> new PartiallyDecryptedEncryptedPCC(contextIds, 1, exponentiatedGamma, exponentiationProofs)),
				() -> assertDoesNotThrow(() -> new PartiallyDecryptedEncryptedPCC(contextIds, 2, exponentiatedGamma, exponentiationProofs)),
				() -> assertDoesNotThrow(() -> new PartiallyDecryptedEncryptedPCC(contextIds, 3, exponentiatedGamma, exponentiationProofs)),
				() -> assertDoesNotThrow(() -> new PartiallyDecryptedEncryptedPCC(contextIds, 4, exponentiatedGamma, exponentiationProofs)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new PartiallyDecryptedEncryptedPCC(contextIds, 5, exponentiatedGamma, exponentiationProofs))
		);
	}

}