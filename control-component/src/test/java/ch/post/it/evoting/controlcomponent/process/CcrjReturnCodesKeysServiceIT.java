/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenKeysCCROutput;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

@SpringBootTest
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
@ActiveProfiles("test")
@DisplayName("CcrjReturnCodesKeysService")
class CcrjReturnCodesKeysServiceIT {

	private static final Random random = RandomFactory.createRandom();

	private static String electionEventId;
	private static GqGroup encryptionGroup;

	@Autowired
	private CcrjReturnCodesKeysService ccrjReturnCodesKeysService;

	@BeforeAll
	static void setUpAll(
			@Autowired
			final ElectionEventService electionEventService) {

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();

		encryptionGroup = GroupTestData.getGqGroup();
		electionEventService.save(electionEventId, encryptionGroup);
	}

	@Test
	@DisplayName("return codes keys saves to database")
	void save() {
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(encryptionGroup));
		final ZqElement ccrjReturnCodesGenerationSecretKey = zqGroupGenerator.genRandomZqElementMember();
		final ElGamalMultiRecipientKeyPair ccrjChoiceReturnCodesEncryptionKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(encryptionGroup, 1,
				random);
		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs = IntStream.range(0, ccrjChoiceReturnCodesEncryptionKeyPair.size())
				.mapToObj(i -> new SchnorrProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.collect(toGroupVector());
		final GenKeysCCROutput genKeysCCROutput = new GenKeysCCROutput(ccrjChoiceReturnCodesEncryptionKeyPair, ccrjReturnCodesGenerationSecretKey,
				schnorrProofs);

		assertDoesNotThrow(() -> ccrjReturnCodesKeysService.save(electionEventId, genKeysCCROutput));
	}
}