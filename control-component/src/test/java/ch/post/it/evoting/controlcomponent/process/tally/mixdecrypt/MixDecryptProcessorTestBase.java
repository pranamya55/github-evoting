/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_FILENAME_PATH;
import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_PASSWORD_FILENAME_PATH;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.domain.multitenancy.TenantConstants.TEST_TENANT_ID;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ch.post.it.evoting.controlcomponent.ArtemisSupport;
import ch.post.it.evoting.controlcomponent.TestSigner;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.controlcomponent.process.VerificationCard;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally.SetupTallyCCMOutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.domain.generators.ControlComponentBallotBoxPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsInput;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsOutput;

public class MixDecryptProcessorTestBase extends ArtemisSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptProcessorTestBase.class);
	private static final Map<Integer, MixDecryptResponse> responseMessages = new HashMap<>();
	private static final int NUMBER_OF_VERIFICATION_CARDS = 10;
	private static final int NUMBER_OF_CONFIRMED_VOTES = 10; // Be sure to have NUMBER_OF_CONFIRMED_VOTES <= NUMBER_OF_VERIFICATION_CARDS
	private static final int NUMBER_OF_WRITE_INS_PLUS_ONE = 4;

	protected static GqGroup gqGroup;
	protected static String ballotBoxIdToMix;
	protected static ElectionEventContext electionEventContext;
	protected static ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads;

	private static ElGamalGenerator elGamalGenerator;
	private static SetupComponentPublicKeys setupComponentPublicKeys;
	private static ImmutableList<SetupTallyCCMOutput> setupTallyCCMOutputs;
	private static ImmutableList<String> verificationCardIds;
	private static ImmutableList<EncryptedVerifiableVote> confirmedVotes;

	static {
		// Generates initial election data shared by all control-components.
		setUpElection();
	}

	@BeforeAll
	static void setUpAll(
			@Autowired
			final ElectionEventService electionEventService,
			@Autowired
			final ElectionEventContextService electionEventContextService,
			@Autowired
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			@Autowired
			final EncryptedVerifiableVoteService encryptedVerifiableVoteService,
			@Autowired
			final VerificationCardService verificationCardService,
			@Autowired
			final MixnetInitialCiphertextsService mixnetInitialCiphertextsService,
			@Autowired
			final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm,
			@Autowired
			final ContextHolder contextHolder) {

		contextHolder.setTenantId(TEST_TENANT_ID);

		// Save election event.
		final String electionEventId = electionEventContext.electionEventId();
		electionEventService.save(electionEventId, gqGroup);
		LOGGER.info("Election event saved.");

		// Save election context
		electionEventContextService.save(electionEventContext);
		setupComponentPublicKeysService.save(electionEventId, setupComponentPublicKeys);
		LOGGER.info("Election event context and public keys saved.");

		// Save cards.
		final ImmutableList<VerificationCardSetContext> verificationCardSetContexts = electionEventContext.verificationCardSetContexts();
		final String verificationCardSetId = verificationCardSetContexts.get(0).getVerificationCardSetId();
		final ImmutableList<VerificationCard> verificationCards = verificationCardIds.stream()
				.map(verificationCardId -> new VerificationCard(verificationCardId, verificationCardSetId, elGamalGenerator.genRandomPublicKey(1)))
				.collect(toImmutableList());
		verificationCardService.saveAll(verificationCards);
		LOGGER.info("Verification cards saved.");

		// Save votes.
		confirmedVotes.forEach(encryptedVerifiableVoteService::save);
		LOGGER.info("Encrypted verifiable votes saved.");

		ballotBoxIdToMix = verificationCardSetContexts.get(0).getBallotBoxId();

		// Save getMixnetInitialCiphertextsOutputs and provide all 4 controlComponentVotesHashPayloads.
		setUpGetMixnetInitialCiphertexts(mixnetInitialCiphertextsService, getMixnetInitialCiphertextsAlgorithm, electionEventId);
	}

	private static void setUpElection() {
		final BigInteger p = new BigInteger(
				"BFF67CCCAE0F61B38BA70AD736CFA8EA284B5D6CAEBF2FED2FC88D0ADFF9E2B220BFD9CCDA59BD3BD52B12CDFCCF41AA3D9BF81F95A7D59452690BF45F7993BE760ABBCA3E29705D473A66638DCD6EA78663C0DB91E3E0AB1DFE1AFF25181D4D2C3BA059F9131D95D37F431233EA2276E052C960DCB130F9DFFDC0BE977C9947E7AE05EA516AA81B2528FEF03625ACFCF495C3AB5D5F176E06F1382AE96A470321092C0C1C02A196AB4DA20D3605B4E72A5CFD16CF9381C83513EBD18A8A4A21BF95B864EDA4C0214583E99A3180F7A561F19D451BC4354E7A284DC7EB0C5A05DC58856C6DC8CF3A57B42D866D85F453D1BD8CC61117FB606A40AF0A0EF76D603C7A307C0B8854355D5836774C6BB12238E09806782A487BB9888AE1DB54DECA3FEC374D30CC9A722D3052585069D212B62FD6758710337CA17411E82FF7E7E7B754F4C9F3A1C49AA15E0D0A0E9B05A2EA880216D052B780E68168CA336309D3C1802A278AFCF1C0F8FA3381C145DA0864892221B960ECD6D46165E057B55EEB",
				16);
		final BigInteger q = new BigInteger(
				"5FFB3E665707B0D9C5D3856B9B67D4751425AEB6575F97F697E446856FFCF159105FECE66D2CDE9DEA958966FE67A0D51ECDFC0FCAD3EACA293485FA2FBCC9DF3B055DE51F14B82EA39D3331C6E6B753C331E06DC8F1F0558EFF0D7F928C0EA6961DD02CFC898ECAE9BFA18919F5113B702964B06E58987CEFFEE05F4BBE4CA3F3D702F528B5540D92947F781B12D67E7A4AE1D5AEAF8BB703789C1574B52381908496060E0150CB55A6D1069B02DA73952E7E8B67C9C0E41A89F5E8C5452510DFCADC3276D26010A2C1F4CD18C07BD2B0F8CEA28DE21AA73D1426E3F5862D02EE2C42B636E4679D2BDA16C336C2FA29E8DEC663088BFDB035205785077BB6B01E3D183E05C42A1AAEAC1B3BA635D8911C704C033C15243DDCC44570EDAA6F651FF61BA698664D391698292C2834E9095B17EB3AC38819BE50BA08F417FBF3F3DBAA7A64F9D0E24D50AF0685074D82D17544010B68295BC07340B46519B184E9E0C01513C57E78E07C7D19C0E0A2ED0432449110DCB0766B6A30B2F02BDAAF75",
				16);
		final BigInteger g = new BigInteger(
				"3",
				16);
		gqGroup = new GqGroup(p, q, g);
		elGamalGenerator = new ElGamalGenerator(gqGroup);
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);
		final int l = 4;

		// Generate key pairs for each control-component.
		final Random random = RandomFactory.createRandom();
		final GroupVector<SchnorrProof, ZqGroup> ccrjSchnorrProofs = IntStream.range(0, l)
				.mapToObj(ignored -> new SchnorrProof(
						zqGroupGenerator.genRandomZqElementMember(),
						zqGroupGenerator.genRandomZqElementMember()))
				.collect(GroupVector.toGroupVector());
		setupTallyCCMOutputs = ControlComponentNode.ids().stream()
				.map(nodeId -> new SetupTallyCCMOutput.Builder()
						.setElGamalMultiRecipientKeyPair(ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, l, random))
						.setSchnorrProofs(ccrjSchnorrProofs)
						.build())
				.collect(toImmutableList());
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys = setupTallyCCMOutputs.stream()
				.map(SetupTallyCCMOutput::getCcmjElectionKeyPair)
				.map(ElGamalMultiRecipientKeyPair::getPublicKey)
				.collect(GroupVector.toGroupVector());
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccrChoiceReturnCodesEncryptionPublicKeys = ControlComponentNode.ids().stream()
				.map(nodeId -> ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, l, random))
				.map(ElGamalMultiRecipientKeyPair::getPublicKey)
				.collect(GroupVector.toGroupVector());

		final int numberOfEligibleVoters = 2;
		electionEventContext = new ElectionEventContextPayloadGenerator(gqGroup).generate(l, NUMBER_OF_WRITE_INS_PLUS_ONE, numberOfEligibleVoters)
				.getElectionEventContext();
		setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator(gqGroup).generate(ccrChoiceReturnCodesEncryptionPublicKeys,
				ccmElectionPublicKeys).getSetupComponentPublicKeys();

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		verificationCardIds = IntStream.range(0, NUMBER_OF_VERIFICATION_CARDS)
				.mapToObj(i -> uuidGenerator.generate())
				.collect(toImmutableList());

		final String electionEventId = electionEventContext.electionEventId();
		final String verificationCardSetId = electionEventContext.verificationCardSetContexts().get(0).getVerificationCardSetId();
		final ControlComponentBallotBoxPayloadGenerator controlComponentBallotBoxPayloadGenerator = new ControlComponentBallotBoxPayloadGenerator(
				gqGroup);
		confirmedVotes = verificationCardIds.stream()
				.limit(NUMBER_OF_CONFIRMED_VOTES)
				.map(verificationCardId -> {
					final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
					return controlComponentBallotBoxPayloadGenerator.generateEncryptedVerifiableVote(contextIds, l, NUMBER_OF_WRITE_INS_PLUS_ONE);
				})
				.collect(toImmutableList());
		LOGGER.info("Election data generated.");
	}

	private static void setUpGetMixnetInitialCiphertexts(final MixnetInitialCiphertextsService mixnetInitialCiphertextsService,
			final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm, final String electionEventId) {

		final ImmutableMap<String, ElGamalMultiRecipientCiphertext> encryptedConfirmedVotes = confirmedVotes.stream()
				.collect(toImmutableMap(vote -> vote.contextIds().verificationCardId(), EncryptedVerifiableVote::encryptedVote));

		final GetMixnetInitialCiphertextsContext getMixnetInitialCiphertextsContext = new GetMixnetInitialCiphertextsContext(gqGroup,
				encryptedConfirmedVotes.size(), NUMBER_OF_WRITE_INS_PLUS_ONE, setupComponentPublicKeys.electionPublicKey());
		final GetMixnetInitialCiphertextsInput getMixnetInitialCiphertextsInput = new GetMixnetInitialCiphertextsInput(encryptedConfirmedVotes);

		final GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertextsOutput = getMixnetInitialCiphertextsAlgorithm.getMixnetInitialCiphertexts(
				getMixnetInitialCiphertextsContext, getMixnetInitialCiphertextsInput);
		mixnetInitialCiphertextsService.save(electionEventId, ballotBoxIdToMix, getMixnetInitialCiphertextsOutput);

		// For simplicity, we give the encryptedConfirmedVotesHash of the first node to all nodes. In production, each node computes his own encryptedConfirmedVotesHash.
		controlComponentVotesHashPayloads = genControlComponentVotesHashPayloads(electionEventId, ballotBoxIdToMix,
				getMixnetInitialCiphertextsOutput.encryptedConfirmedVotesHash());
	}

	private static ImmutableList<ControlComponentVotesHashPayload> genControlComponentVotesHashPayloads(final String electionEventId,
			final String ballotBoxId, final String encryptedConfirmedVotesHash) {
		return ControlComponentNode.ids().stream()
				.map(nodeId -> {
					final ControlComponentVotesHashPayload controlComponentVotesHashPayload = new ControlComponentVotesHashPayload(electionEventId,
							ballotBoxIdToMix, nodeId, encryptedConfirmedVotesHash);

					try {
						final TestSigner controlComponentSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH,
								Alias.getControlComponentByNodeId(nodeId));
						controlComponentSigner.sign(controlComponentVotesHashPayload,
								ChannelSecurityContextData.controlComponentVotesHash(nodeId, electionEventId, ballotBoxId));
					} catch (final IOException | SignatureException e) {
						throw new IllegalStateException("Failed to test sign control component votes hash payload", e);
					}

					return controlComponentVotesHashPayload;
				}).collect(toImmutableList());
	}

	protected static SetupTallyCCMOutput getSetupTallyCCMOutput(final int nodeId) {
		return setupTallyCCMOutputs.get(nodeId - 1);
	}

	protected void addResponseMessage(final int nodeId, final String electionEventId, final String ballotBoxId,
			final ImmutableByteArray responseMessage) {
		responseMessages.put(nodeId, new MixDecryptResponse(electionEventId, ballotBoxId, responseMessage));
	}

	protected MixDecryptResponse getResponseMessage(final int nodeId) {
		return responseMessages.get(nodeId);
	}

	protected static class MixDecryptResponse {

		private final String electionEventId;
		private final String ballotBoxId;
		private final ImmutableByteArray message;

		MixDecryptResponse(final String electionEventId, final String ballotBoxId, final ImmutableByteArray message) {
			this.electionEventId = electionEventId;
			this.ballotBoxId = ballotBoxId;
			this.message = message;
		}

		public String getElectionEventId() {
			return electionEventId;
		}

		public String getBallotBoxId() {
			return ballotBoxId;
		}

		public ImmutableByteArray getMessage() {
			return message;
		}
	}

}
