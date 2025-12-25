/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

@DisplayName("A GenKeysCCRAlgorithm")
class GenKeysCCRAlgorithmTest {

	private static final int PSI_SUP = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
	private static final int PSI_MAX = RandomFactory.createRandom().genRandomInteger(PSI_SUP - 1) + 1;
	private static final int NODE_ID = 1;
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();

	private static GenKeysCCRContext genKeysCCRContext;
	private static GenKeysCCRAlgorithm genKeysCCRAlgorithm;

	@BeforeAll
	static void setUpAll() {
		final ZeroKnowledgeProof zeroKnowledgeProof = ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
		genKeysCCRAlgorithm = new GenKeysCCRAlgorithm(RandomFactory.createRandom(), zeroKnowledgeProof);

		final GqGroup encryptionGroup = GroupTestData.getLargeGqGroup();
		genKeysCCRContext = new GenKeysCCRContext(encryptionGroup, NODE_ID, ELECTION_EVENT_ID, PSI_MAX);
	}

	@Test
	@DisplayName("valid parameter does not throw")
	void validParamDoesNotThrow() {
		assertDoesNotThrow(() -> genKeysCCRAlgorithm.genKeysCCR(genKeysCCRContext));
	}

	@Test
	@DisplayName("null context throws NullPointerException")
	void nullContextThrows() {
		assertThrows(NullPointerException.class, () -> genKeysCCRAlgorithm.genKeysCCR(null));
	}

}
