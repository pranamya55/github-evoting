/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;

class BoardPasswordHashServiceTest {

	private static BoardPasswordHashService boardPasswordHashService;
	private final SafePasswordHolder boardPassword1 = new SafePasswordHolder("Password_LongBoardPassword1".toCharArray());
	private final SafePasswordHolder boardPassword2 = new SafePasswordHolder("Password_LongBoardPassword2".toCharArray());
	private final ImmutableList<SafePasswordHolder> boardPasswords = ImmutableList.of(boardPassword1, boardPassword2);

	@BeforeAll
	static void setUpAll() {
		final Argon2Profile profile = Argon2Profile.TEST;
		final Argon2 argon2 = Argon2Factory.createArgon2(profile);
		boardPasswordHashService = new BoardPasswordHashService(argon2);
	}

	@Test
	void cyclicTest() {
		final ImmutableList<SafePasswordHolder> passwordsForHashing = boardPasswords.stream()
				.map(SafePasswordHolder::copy)
				.collect(toImmutableList());
		final ImmutableList<ImmutableByteArray> hashes = assertDoesNotThrow(() -> boardPasswordHashService.hashPasswords(passwordsForHashing));
		assertAll(
				() -> assertTrue(boardPasswordHashService.verifyPassword(boardPassword1, hashes.get(0))),
				() -> assertTrue(boardPasswordHashService.verifyPassword(boardPassword2, hashes.get(1)))
		);
	}

	@Nested
	class HashPasswords {
		@Test
		void hashPasswordsHappyPath() {
			final ImmutableList<ImmutableByteArray> hashes = assertDoesNotThrow(() -> boardPasswordHashService.hashPasswords(boardPasswords));
			assertEquals(2, hashes.size());
		}

		@Test
		void hashPasswordsNullPasswordsThrows() {
			assertThrows(NullPointerException.class, () -> boardPasswordHashService.hashPasswords(null));
		}
	}

	@Nested
	class VerifyPassword {

		@Test
		void verifyPasswordHappyPath() {
			final ImmutableList<SafePasswordHolder> passwordsForHashing = boardPasswords.stream().map(SafePasswordHolder::copy)
					.collect(toImmutableList());
			final ImmutableList<ImmutableByteArray> hashes = boardPasswordHashService.hashPasswords(passwordsForHashing);
			assertAll(
					() -> assertTrue(
							boardPasswordHashService.verifyPassword(boardPassword1, hashes.get(0))),
					() -> assertTrue(
							boardPasswordHashService.verifyPassword(boardPassword2, hashes.get(1)))
			);
		}

		@Test
		void verifyPasswordNullBoardMemberHashThrows() {
			assertThrows(NullPointerException.class, () -> boardPasswordHashService.verifyPassword(boardPassword1, null));
		}

		@Test
		void verifyPasswordInvalidBoardMemberHashThrows() {
			final ImmutableList<ImmutableByteArray> hashes = boardPasswordHashService.hashPasswords(boardPasswords);
			final SafePasswordHolder invalidPassword = new SafePasswordHolder("invalid".toCharArray());
			final ImmutableByteArray passwordHash = hashes.get(0);
			assertFalse(boardPasswordHashService.verifyPassword(invalidPassword, passwordHash));
		}
	}

}
