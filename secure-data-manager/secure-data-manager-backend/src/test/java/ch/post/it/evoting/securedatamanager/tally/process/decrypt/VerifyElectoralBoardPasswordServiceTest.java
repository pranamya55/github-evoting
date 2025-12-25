/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.configuration.ElectoralBoardHashesPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.securedatamanager.shared.process.BoardPasswordHashService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardHashesPayloadService;
import ch.post.it.evoting.securedatamanager.tally.process.VerifyElectoralBoardPasswordService;

@ExtendWith(MockitoExtension.class)
class VerifyElectoralBoardPasswordServiceTest {

	private static final String ELECTION_EVENT_ID = "0B149CFDAAD04B04B990C3B1D4CA7639";
	private static final SafePasswordHolder ELECTORAL_BOARD_1_PASSWORD = new SafePasswordHolder("Password_ElectoralBoard1_2".toCharArray());
	private static final SafePasswordHolder ELECTORAL_BOARD_2_PASSWORD = new SafePasswordHolder("Password_ElectoralBoard2_2".toCharArray());
	private static final ImmutableList<SafePasswordHolder> ELECTORAL_BOARD_PASSWORDS = ImmutableList.of(ELECTORAL_BOARD_1_PASSWORD,
			ELECTORAL_BOARD_2_PASSWORD);
	private static final ImmutableList<String> ELECTORAL_BOARD_HASHES = ImmutableList.of(
			"Q+Lo83LsPZejxGFv4rDvOBLe72gvMcJw6yT7/HkG8PU0ZWZXWWRzYzZUS1gvZndRODlrdWckRGczd1g1ZEUzZ2Q1UGY3cXV0VlNockpCMHdjeTQxM0RJL3J2OG4zWUJHTQ==",
			"4LAw7fKOvybxsxRqN8WcFtUCN4FcUgvwSUmmeZUHOQtuaDBkYVRQd1dTOWJMVjRyYU5DbVEkcGZnUmJ6MWR2RU1XdE50YnRTbHl1Zjk3RUwyeWpncHRpaThPZ2hJTkgrTQ==");
	private static final CryptoPrimitivesSignature ELECTORAL_BOARD_HASHES_SIGNATURE = new CryptoPrimitivesSignature(
			ImmutableByteArray.of((byte) 1, (byte) 2));
	private static final ElectoralBoardHashesPayload ELECTORAL_BOARD_HASHES_PAYLOAD = new ElectoralBoardHashesPayload(ELECTION_EVENT_ID,
			ELECTORAL_BOARD_HASHES, ELECTORAL_BOARD_HASHES_SIGNATURE);
	private static final ElectoralBoardHashesPayloadService electoralBoardHashesPayloadService = mock(ElectoralBoardHashesPayloadService.class);
	@SuppressWarnings("unchecked")
	private static final SignatureKeystore<Alias> signatureKeystoreService = mock(SignatureKeystore.class);
	private static final BoardPasswordHashService boardPasswordHashService = mock(BoardPasswordHashService.class);
	private static VerifyElectoralBoardPasswordService verifyElectoralBoardPasswordService;

	@BeforeAll
	static void setUp() throws SignatureException {
		verifyElectoralBoardPasswordService = new VerifyElectoralBoardPasswordService(
				boardPasswordHashService,
				signatureKeystoreService,
				electoralBoardHashesPayloadService);
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(true);
	}

	@Test
	void verifyElectoralBoardMembersPasswordsHappyPath() {
		when(electoralBoardHashesPayloadService.load(ELECTION_EVENT_ID)).thenReturn(ELECTORAL_BOARD_HASHES_PAYLOAD);
		when(boardPasswordHashService.verifyPassword(ELECTORAL_BOARD_PASSWORDS.get(0),
				ELECTORAL_BOARD_HASHES_PAYLOAD.getElectoralBoardHashes().get(0))).thenReturn(true);
		when(boardPasswordHashService.verifyPassword(ELECTORAL_BOARD_PASSWORDS.get(1),
				ELECTORAL_BOARD_HASHES_PAYLOAD.getElectoralBoardHashes().get(1))).thenReturn(true);

		assertTrue(verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(ELECTION_EVENT_ID, 0, ELECTORAL_BOARD_PASSWORDS.get(0)));
		assertTrue(verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(ELECTION_EVENT_ID, 1, ELECTORAL_BOARD_PASSWORDS.get(1)));
	}

	@Test
	void verifyElectoralBoardMembersPasswordsInvalidPassword() {
		final SafePasswordHolder notEB1Password = new SafePasswordHolder("Not_EB1_password".toCharArray());
		when(electoralBoardHashesPayloadService.load(ELECTION_EVENT_ID)).thenReturn(ELECTORAL_BOARD_HASHES_PAYLOAD);
		when(boardPasswordHashService.verifyPassword(notEB1Password, ELECTORAL_BOARD_HASHES_PAYLOAD.getElectoralBoardHashes().get(0)))
				.thenReturn(false);
		assertFalse(verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(ELECTION_EVENT_ID, 0, notEB1Password));
	}

	@Test
	void verifyElectoralBoardMembersPasswordsWithNullParam() {
		assertThrows(NullPointerException.class,
				() -> verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(null, 0, ELECTORAL_BOARD_1_PASSWORD));
		assertThrows(NullPointerException.class,
				() -> verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(ELECTION_EVENT_ID, 0, null));
	}

	@Test
	void verifyElectoralBoardMembersPasswordsInvalidParam() {
		when(electoralBoardHashesPayloadService.load(ELECTION_EVENT_ID)).thenReturn(ELECTORAL_BOARD_HASHES_PAYLOAD);

		assertThrows(IllegalArgumentException.class,
				() -> verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(ELECTION_EVENT_ID, -1, ELECTORAL_BOARD_1_PASSWORD));
		assertThrows(IllegalArgumentException.class,
				() -> verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(ELECTION_EVENT_ID, 5, ELECTORAL_BOARD_1_PASSWORD));
	}
}
