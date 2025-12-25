/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_FILENAME_PATH;
import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_PASSWORD_FILENAME_PATH;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.ArtemisSupport;
import ch.post.it.evoting.controlcomponent.TestSigner;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.VerificationCard;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote.CreateLVCCShareOutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCRequestPayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponentlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.LongVoteCastReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

class LongVoteCastReturnCodeShareVerifyProcessorIT extends ArtemisSupport {

	private static ControlComponenthlVCCRequestPayload controlComponenthlVCCRequestPayload;
	private static ContextIds contextIds;
	private static GqGroup gqGroup;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeAll
	@SuppressWarnings("java:S117")
	static void setUpAll(
			@Autowired
			final ElectionEventService electionEventService,
			@Autowired
			final VerificationCardSetService verificationCardSetService,
			@Autowired
			final Hash hash,
			@Autowired
			final ElectionEventContextService electionEventContextService,
			@Autowired
			final ElectionEventStateService electionEventStateService,
			@Autowired
			final VerificationCardService verificationCardService,
			@Autowired
			final VerificationCardStateService verificationCardStateService,
			@Autowired
			final LVCCShareService lvccShareService) throws IOException, SignatureException {

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContext electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();
		gqGroup = electionEventContext.encryptionGroup();

		// Save election event. The election must be in the CONFIGURED state for this processor.
		electionEventService.save(electionEventId, gqGroup);
		electionEventStateService.updateElectionEventState(electionEventId, ElectionEventState.CONFIGURED);

		// Save election event context.
		electionEventContextService.save(electionEventContext);

		// Create verification card.
		final String verificationCardSetId = electionEventContext.verificationCardSetContexts().get(0).getVerificationCardSetId();
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String verificationCardId = uuidGenerator.generate();

		final ElGamalMultiRecipientPublicKey publicKey = elGamalGenerator.genRandomPublicKey(1);
		verificationCardService.save(new VerificationCard(verificationCardId, verificationCardSetId, publicKey));

		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);

		// Save allow list.
		final Random random = RandomFactory.createRandom();
		final Alphabet base64Alphabet = Base64Alphabet.getInstance();
		final String hashedLongVoteCastReturnCode = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final ImmutableList<String> otherCCRsHashedLongVoteCastReturnCodes = ImmutableList.of(
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet),
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet),
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet));

		final ImmutableList<String> hlVCC = Stream.concat(Stream.of(hashedLongVoteCastReturnCode), otherCCRsHashedLongVoteCastReturnCodes.stream())
				.collect(toImmutableList());

		final ImmutableList<HashableString> i_aux_list = Stream.of("VerifyLVCCHash", electionEventId, verificationCardSetId, verificationCardId)
				.map(HashableString::from)
				.collect(toImmutableList());
		final HashableList i_aux = HashableList.from(i_aux_list);
		final HashableString hlVCC_id_1 = HashableString.from(hlVCC.get(0));
		final HashableString hlVCC_id_2 = HashableString.from(hlVCC.get(1));
		final HashableString hlVCC_id_3 = HashableString.from(hlVCC.get(2));
		final HashableString hlVCC_id_4 = HashableString.from(hlVCC.get(3));
		final String hhlVCC_id = Base64.getEncoder()
				.encodeToString(hash.recursiveHash(i_aux, hlVCC_id_1, hlVCC_id_2, hlVCC_id_3, hlVCC_id_4).elements());

		final ImmutableList<String> L_lVCC = ImmutableList.of(hhlVCC_id);

		verificationCardSetService.setLongVoteCastReturnCodesAllowList(verificationCardSetId, L_lVCC);

		//Mark card as LCC share created
		verificationCardStateService.setSentVote(verificationCardId);

		final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(gqGroup);

		final ConfirmationKey confirmationKey = new ConfirmationKey(contextIds, new GqGroupGenerator(gqGroup).genMember());

		// Simulate call to CreateLVCCShare algorithm
		verificationCardStateService.incrementConfirmationAttempts(verificationCardId);
		final GqElement longVoteCastReturnCodeShare = gqGroupGenerator.genMember();
		final CreateLVCCShareOutput createLVCCShareOutput = new CreateLVCCShareOutput(longVoteCastReturnCodeShare, "hash", 1);
		lvccShareService.save(confirmationKey, createLVCCShareOutput);

		final int confirmationAttemptId = 0;

		controlComponenthlVCCRequestPayload = new ControlComponenthlVCCRequestPayload(ImmutableList.of(
				getSignedControlComponenthlVCCPayload(confirmationKey, confirmationAttemptId, 1, hashedLongVoteCastReturnCode),
				getSignedControlComponenthlVCCPayload(confirmationKey, confirmationAttemptId, 2,
						otherCCRsHashedLongVoteCastReturnCodes.get(0)),
				getSignedControlComponenthlVCCPayload(confirmationKey, confirmationAttemptId, 3,
						otherCCRsHashedLongVoteCastReturnCodes.get(1)),
				getSignedControlComponenthlVCCPayload(confirmationKey, confirmationAttemptId, 4,
						otherCCRsHashedLongVoteCastReturnCodes.get(2))
		));
	}

	private static ControlComponenthlVCCSharePayload getSignedControlComponenthlVCCPayload(final ConfirmationKey confirmationKey,
			final Integer confirmationAttemptId, final int nodeId, final String hashLongVoteCastCodeShare)
			throws IOException, SignatureException {

		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload =
				new ControlComponenthlVCCSharePayload(gqGroup, nodeId, hashLongVoteCastCodeShare, confirmationKey,
						confirmationAttemptId);

		final TestSigner controlComponentByNodeIdSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH,
				Alias.getControlComponentByNodeId(nodeId));
		controlComponentByNodeIdSigner.sign(controlComponenthlVCCSharePayload,
				ChannelSecurityContextData.controlComponenthlVCCShare(nodeId, contextIds.electionEventId(), contextIds.verificationCardSetId(),
						contextIds.verificationCardId()));

		return controlComponenthlVCCSharePayload;
	}

	@Test
	void happyPath() throws IOException, JMSException {
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, objectMapper.writeValueAsBytes(controlComponenthlVCCRequestPayload),
				jmsMessage -> {
					jmsMessage.setJMSCorrelationID(correlationId);
					jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, controlComponenthlVCCRequestPayload.getClass().getName());
					jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
					jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
					return jmsMessage;
				});

		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		assertNotNull(responseMessage);

		final ControlComponentlVCCSharePayload responsePayload =
				objectMapper.readValue(responseMessage.getBody(byte[].class), ControlComponentlVCCSharePayload.class);

		assertTrue(responsePayload.isVerified());
		assertEquals(gqGroup, responsePayload.getEncryptionGroup());
		assertNotNull(responsePayload.getSignature());

		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare = responsePayload.getLongVoteCastReturnCodeShare()
				.orElseThrow(() -> new IllegalStateException("We checked is verified, shouldn't reach here"));
		assertNotNull(longVoteCastReturnCodeShare);

		assertEquals(1, longVoteCastReturnCodeShare.nodeId());
		assertEquals(contextIds.electionEventId(), longVoteCastReturnCodeShare.electionEventId());
		assertEquals(contextIds.verificationCardSetId(), longVoteCastReturnCodeShare.verificationCardSetId());
		assertEquals(contextIds.verificationCardId(), longVoteCastReturnCodeShare.verificationCardId());
	}
}

