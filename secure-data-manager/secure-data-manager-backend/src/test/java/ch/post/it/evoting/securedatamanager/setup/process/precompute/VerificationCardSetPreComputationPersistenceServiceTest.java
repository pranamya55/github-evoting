/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.precompute;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BIRTH_YEAR;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SIGNING_KEY_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement.PrimeGqElementFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationsType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ContestType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ExtendedAuthenticationKeyType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ExtendedAuthenticationKeysDefinitionType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ExtendedAuthenticationKeysType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.RegisterType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoterType;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKeyPayloadService;
import ch.post.it.evoting.securedatamanager.setup.process.VoterInitialCodesPayloadService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.DeriveBaseAuthenticationChallengeAlgorithm;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.DeriveCredentialIdAlgorithm;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenVerDatOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GetVoterAuthenticationDataAlgorithm;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GetVoterAuthenticationDataService;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentTallyDataPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

@ExtendWith(MockitoExtension.class)
class VerificationCardSetPreComputationPersistenceServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String AUTHORIZATION_ID = "9997c020-cc5c-33b4-b7ff-0696c2d36092";
	private static final String VERIFICATION_CARD_ID = uuidGenerator.generate();
	private static final String BALLOT_BOX_ID = uuidGenerator.generate();
	private static final String VOTING_CARD_ID_SUFFIX = "A13ECA37FA";

	private static final BigInteger P = new BigInteger(
			"4688924687101842747043789622943639451238379583421515083490992727662966887592566806385937818968022125389969548661587837554751555551125316712096348517237427873704786293289327916804886782297937842786976257115026543950184245775805780806466220397371589271121288423507399259602000829340247207828163695625078614543895796426520749021726851028889703185286047412971103954221566262244551464311742715148749253272752397456639673970809661134301137187709504653404337846916552289737947354931132013578668381751783880929044628310827581093820299384525520498865891279620245835238216578248104372858793870818677470303875907983918128169426975079981824932085456362035949171853839641915630629131160070262536188292899390408699336228786000874475423989873636678599713537438189405718684830889918695758274995475275472436678812224043372657209577592652124268309300980776740510688847875393810499063675510899395946989871928027318991345508642195522561212328039");
	private static final BigInteger Q = new BigInteger(
			"2344462343550921373521894811471819725619189791710757541745496363831483443796283403192968909484011062694984774330793918777375777775562658356048174258618713936852393146644663958402443391148968921393488128557513271975092122887902890403233110198685794635560644211753699629801000414670123603914081847812539307271947898213260374510863425514444851592643023706485551977110783131122275732155871357574374626636376198728319836985404830567150568593854752326702168923458276144868973677465566006789334190875891940464522314155413790546910149692262760249432945639810122917619108289124052186429396935409338735151937953991959064084713487539990912466042728181017974585926919820957815314565580035131268094146449695204349668114393000437237711994936818339299856768719094702859342415444959347879137497737637736218339406112021686328604788796326062134154650490388370255344423937696905249531837755449697973494935964013659495672754321097761280606164019");
	private static final BigInteger G = new BigInteger("2");

	private static GqGroup gqGroup;
	private static String electionEventId;
	private static String verificationCardSetId;
	private static ElectionEventContextPayload electionEventContextPayload;

	private final Hash hash = HashFactory.createHash();
	private final Argon2 argon2 = Argon2Factory.createArgon2(Argon2Profile.TEST);
	private final ElGamal elGamal = ElGamalFactory.createElGamal();
	private final DeriveCredentialIdAlgorithm deriveCredentialIdAlgorithm = new DeriveCredentialIdAlgorithm(hash, BaseEncodingFactory.createBase16(),
			argon2);
	private final EvotingConfigService evotingConfigService = mock(EvotingConfigService.class);
	private final DeriveBaseAuthenticationChallengeAlgorithm deriveBaseAuthenticationChallengeAlgorithm = new DeriveBaseAuthenticationChallengeAlgorithm(
			hash, argon2, BaseEncodingFactory.createBase64());
	@Spy
	private final GetVoterAuthenticationDataAlgorithm getVoterAuthenticationDataAlgorithm = new GetVoterAuthenticationDataAlgorithm(
			deriveCredentialIdAlgorithm, deriveBaseAuthenticationChallengeAlgorithm);
	@Mock
	private ElectionEventService electionEventService;
	@Mock
	private ElectionEventContextPayloadService electionEventContextPayloadService;
	@Mock
	private SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;
	@Mock
	private SignatureKeystore<Alias> signatureKeystoreService;
	@Mock
	private SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService;
	@Mock
	private VerificationCardSecretKeyPayloadService verificationCardSecretKeyPayloadService;
	@Mock
	private VoterInitialCodesPayloadService voterInitialCodesPayloadService;
	@Mock
	private SetupComponentVoterAuthenticationPayloadService setupComponentVoterAuthenticationPayloadService;
	@Mock
	private VerificationCardSetService verificationCardSetService;

	private VerificationCardSetPreComputationPersistenceService verificationCardSetPrecomputationPersistenceService;

	@BeforeEach
	void setUp() {
		final GetVoterAuthenticationDataService getVoterAuthenticationDataService = new GetVoterAuthenticationDataService(
				getVoterAuthenticationDataAlgorithm);
		verificationCardSetPrecomputationPersistenceService = new VerificationCardSetPreComputationPersistenceService(
				VOTING_CARD_ID_SUFFIX,
				electionEventService,
				evotingConfigService,
				signatureKeystoreService,
				verificationCardSetService,
				voterInitialCodesPayloadService,
				getVoterAuthenticationDataService,
				electionEventContextPayloadService,
				setupComponentTallyDataPayloadService,
				verificationCardSecretKeyPayloadService,
				setupComponentVerificationDataPayloadFileRepository,
				setupComponentVoterAuthenticationPayloadService
		);

		gqGroup = new GqGroup(P, Q, G);
		electionEventContextPayload = new ElectionEventContextPayloadGenerator(gqGroup).generate(12);
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		verificationCardSetId = electionEventContextPayload.getElectionEventContext()
				.verificationCardSetContexts()
				.get(0)
				.getVerificationCardSetId();
	}

	@RepeatedTest(100)
	void getVotingCardTest() {
		final String votingCardId = verificationCardSetPrecomputationPersistenceService.getVotingCardId(VERIFICATION_CARD_ID);
		assertEquals(ID_LENGTH, votingCardId.length());
		assertEquals(VOTING_CARD_ID_SUFFIX, votingCardId.substring(ID_LENGTH - VOTING_CARD_ID_SUFFIX.length()));
	}

	@Nested
	class PersistPrecomputationPayloadsTest {
		@BeforeEach
		void setUp() throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
			Security.addProvider(new BouncyCastleProvider());

			// Generate the signing key pair.
			final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
			generator.initialize(SIGNING_KEY_LENGTH);

			when(electionEventService.exists(electionEventId)).thenReturn(true);
			when(electionEventContextPayloadService.load(any())).thenReturn(electionEventContextPayload);
			when(evotingConfigService.load()).thenReturn(getConfiguration());
			doNothing().when(setupComponentVerificationDataPayloadFileRepository).remove(electionEventId, verificationCardSetId);
			doNothing().when(setupComponentVerificationDataPayloadFileRepository).store(any());
			when(signatureKeystoreService.generateSignature(any(), any())).thenReturn(
					new ImmutableByteArray(electionEventId.getBytes(StandardCharsets.UTF_8)));
			doNothing().when(setupComponentTallyDataPayloadService).save(any());
			doNothing().when(verificationCardSecretKeyPayloadService).save(any());
			doNothing().when(voterInitialCodesPayloadService).save(any(), any());
			doNothing().when(setupComponentVoterAuthenticationPayloadService).save(any());
		}

		@Test
		void persistPrecomputationPayloads() {
			final PrecomputeContext precomputeContext = new PrecomputeContext(electionEventId, BALLOT_BOX_ID, verificationCardSetId);
			final ImmutableList<GenVerDatOutput> genVerDatOutputs = ImmutableList.of(getGenVerDatOutput(
							uuidGenerator.generate(),
							uuidGenerator.generate(),
							uuidGenerator.generate()),
					getGenVerDatOutput(
							uuidGenerator.generate(),
							uuidGenerator.generate(),
							uuidGenerator.generate()),
					getGenVerDatOutput(
							uuidGenerator.generate(),
							uuidGenerator.generate(),
							uuidGenerator.generate()),
					getGenVerDatOutput(
							uuidGenerator.generate(),
							uuidGenerator.generate(),
							uuidGenerator.generate()));

			final BallotBoxEntity ballotBoxEntity = new BallotBoxEntity(BALLOT_BOX_ID, null, "Ballot Box Description", null, null, 1, false);
			when(verificationCardSetService.getVerificationCardSet(verificationCardSetId)).thenReturn(
					new VerificationCardSetEntity(verificationCardSetId, null, ballotBoxEntity, "", "", "vcs_" + AUTHORIZATION_ID, 1, null));

			assertDoesNotThrow(
					() -> verificationCardSetPrecomputationPersistenceService.persistPreComputationPayloads(precomputeContext, genVerDatOutputs));

			verify(setupComponentVerificationDataPayloadFileRepository, times(genVerDatOutputs.size())).store(any());
			verify(verificationCardSecretKeyPayloadService).save(any());
			verify(voterInitialCodesPayloadService).save(any(), any());
			verify(setupComponentTallyDataPayloadService).save(any());
			verify(setupComponentVoterAuthenticationPayloadService).save(any());
		}

		private static Configuration getConfiguration() {
			final AuthorizationType authorizationType = new AuthorizationType();
			authorizationType.setAuthorizationIdentification(AUTHORIZATION_ID);
			final AuthorizationType authorizationTypeOtherIdentification = new AuthorizationType();
			authorizationTypeOtherIdentification.setAuthorizationIdentification("8887c020-cc5c-33b4-b7ff-0696c2d36092");
			final AuthorizationsType authorizationsType = new AuthorizationsType();
			authorizationsType.setAuthorization(List.of(authorizationType, authorizationTypeOtherIdentification));

			final ExtendedAuthenticationKeyType extendedAuthenticationKeyTypeBirthDate = new ExtendedAuthenticationKeyType();
			extendedAuthenticationKeyTypeBirthDate.setValue("1970");
			extendedAuthenticationKeyTypeBirthDate.setName(BIRTH_YEAR);
			final ExtendedAuthenticationKeysType extendedAuthenticationKeysTypeBirthDate = new ExtendedAuthenticationKeysType();
			extendedAuthenticationKeysTypeBirthDate.setExtendedAuthenticationKey(List.of(extendedAuthenticationKeyTypeBirthDate));

			final ExtendedAuthenticationKeyType extendedAuthenticationKeyTypeBirthYear = new ExtendedAuthenticationKeyType();
			extendedAuthenticationKeyTypeBirthYear.setValue("1970");
			extendedAuthenticationKeyTypeBirthYear.setName(BIRTH_YEAR);
			final ExtendedAuthenticationKeysType extendedAuthenticationKeysTypeBirthYear = new ExtendedAuthenticationKeysType();
			extendedAuthenticationKeysTypeBirthYear.setExtendedAuthenticationKey(List.of(extendedAuthenticationKeyTypeBirthYear));

			final ImmutableList<ExtendedAuthenticationKeysType> extendedAuthenticationKeysTypes = ImmutableList.of(
					extendedAuthenticationKeysTypeBirthDate,
					extendedAuthenticationKeysTypeBirthYear
			);

			final List<VoterType> voterTypeList = new ArrayList<>(IntStream.range(0, 12).mapToObj(i -> {
				final VoterType voterType = new VoterType();
				voterType.setExtendedAuthenticationKeys(extendedAuthenticationKeysTypes.get(i % 2));
				voterType.setVoterIdentification(String.valueOf(1000000 + i));
				voterType.setAuthorization("9997c020-cc5c-33b4-b7ff-0696c2d36092");
				return voterType;
			}).toList());
			final VoterType voterTypeOtherAuthorization = new VoterType();
			voterTypeOtherAuthorization.setAuthorization("a1c5cfff-4ad0-3019-a54b-868131a02e9d");
			voterTypeList.add(voterTypeOtherAuthorization);
			final RegisterType registerType = new RegisterType();
			registerType.setVoter(voterTypeList);

			final Configuration configuration = new Configuration();
			configuration.setAuthorizations(authorizationsType);
			configuration.setRegister(registerType);
			configuration.setContest(
					new ContestType().withExtendedAuthenticationKeys(new ExtendedAuthenticationKeysDefinitionType().withKeyName(BIRTH_YEAR)));

			return configuration;
		}

		private GenVerDatOutput getGenVerDatOutput(final String verificationCardId1, final String verificationCardId2,
				final String verificationCardId3) {
			final ImmutableList<String> verificationCardIds = ImmutableList.of(verificationCardId1, verificationCardId2, verificationCardId3);
			final ImmutableList<String> startVotingKeys = ImmutableList.of(
					ElectionSetupUtils.genStartVotingKey(),
					ElectionSetupUtils.genStartVotingKey(),
					ElectionSetupUtils.genStartVotingKey());
			final ImmutableList<String> ballotCastingKeys = random.genUniqueDecimalStrings(9, 3);
			final ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, 1, RandomFactory.createRandom());
			final ImmutableList<ElGamalMultiRecipientKeyPair> keyPairs = Stream.generate(() -> keyPair).limit(3).collect(toImmutableList());
			final ImmutableList<String> allowList = ImmutableList.of("+ivmzla8ALXHkq4ssfQU9wlE8GvwUOHFDik3MYW5D4I=",
					"+oICJQGqd+n1qyxRmfgfZZkw4+HpR7wxMXlzeXu5yXY=",
					"/G9WM/QYtDypeTX145qZBSvu3d6n9xE6nqFzh1hCq80=", "/HJU7k/zhGihPP6izDvl3Xtax0Uhh9vNwH8JbCg9gWU=",
					"/TJeRo+zUgxTYDkRuTAzUQ43OYS92ze/aCHfst8vmiA=", "/dzRBCJL3nmsjMtRmjlVRm0IS+bUSJEV3gJGOziZvtw=",
					"/gjPZIPxnPFpZh/UrfK3mLs6RyMNq5WL9jVYCaRnW/M=", "/jLd+Zs5hJw7HXe3r8qCSVW/UTA1cSYr2krm+Bua1dU=",
					"/mFIunEisguqvYDgzFkVsbFhX2/jooRmWv/C/4d+vDw=");

			final ElGamalMultiRecipientCiphertext ciphertext = elGamal.neutralElement(3, gqGroup);
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> returnCodes = GroupVector.of(ciphertext, ciphertext, ciphertext);
			final ElGamalMultiRecipientCiphertext ciphertextConfirmationKey = elGamal.neutralElement(1, gqGroup);
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> confirmationKey = GroupVector.of(ciphertextConfirmationKey,
					ciphertextConfirmationKey, ciphertextConfirmationKey);
			GroupVector<PrimeGqElement, GqGroup> smallPrimeGroupMembers;
			do {
				smallPrimeGroupMembers = PrimeGqElementFactory.getSmallPrimeGroupMembers(gqGroup, 3);
			} while (smallPrimeGroupMembers.contains(gqGroup.getGenerator()) || // avoid generator g
					new HashSet<>(smallPrimeGroupMembers).size() != smallPrimeGroupMembers.size()); // avoid duplicates.

			return new GenVerDatOutput.Builder()
					.setVerificationCardIds(verificationCardIds)
					.setStartVotingKeys(startVotingKeys)
					.setVerificationCardKeyPairs(keyPairs)
					.setPartialChoiceReturnCodesAllowList(allowList)
					.setBallotCastingKeys(ballotCastingKeys)
					.setEncryptedHashedPartialChoiceReturnCodes(returnCodes)
					.setEncryptedHashedConfirmationKeys(confirmationKey)
					.build();
		}
	}
}
