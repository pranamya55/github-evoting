/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Hash;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.validations.PasswordValidation;

@Service
public class BoardPasswordHashService {

	private final Argon2 argon2;
	private static final Logger LOGGER = LoggerFactory.getLogger(BoardPasswordHashService.class);

	public BoardPasswordHashService(
			@Qualifier("argon2Standard")
			final Argon2 argon2) {
		this.argon2 = argon2;
	}

	/**
	 * Hashes the passwords of the board members.
	 *
	 * @param boardMembersPasswords the passwords of the board members. Must be a valid BoardPassword and non-null.
	 * @return the hashes of the passwords.
	 * @throws NullPointerException     if the passwords are null.
	 * @throws IllegalArgumentException if any password is invalid.
	 */
	public ImmutableList<ImmutableByteArray> hashPasswords(final ImmutableList<SafePasswordHolder> boardMembersPasswords) {
		checkNotNull(boardMembersPasswords).forEach(pwd -> PasswordValidation.validate(pwd.get(), "board member"));

		// Get the hashes of the passwords
		final ImmutableList<ImmutableByteArray> hashes = boardMembersPasswords.stream().parallel()
				.map(SafePasswordHolder::get)
				.map(this::hashPassword)
				.collect(toImmutableList());

		// Will wipe the passwords after usage
		boardMembersPasswords.stream().parallel().forEach(SafePasswordHolder::clear);
		return hashes;
	}

	/**
	 * Verifies that member password corresponds to the member password hash.
	 *
	 * @param boardMemberPassword  the password of the board member. Must be a valid board password and non-null.
	 * @param boardMemberArgonHash the password hash of the board member. Must be non-null.
	 * @return true if password corresponds to the password hash.
	 * @throws NullPointerException     if password or password hash is null.
	 * @throws IllegalArgumentException if password is invalid.
	 */
	public boolean verifyPassword(final SafePasswordHolder boardMemberPassword, final ImmutableByteArray boardMemberArgonHash) {
		try {
			PasswordValidation.validate(boardMemberPassword.get(), "board member");
		} catch (final Exception e) {
			LOGGER.error("Password does not comply with policy requirements.", e);
			return false;
		}
		checkNotNull(boardMemberArgonHash);

		final ImmutableByteArray boardMemberHashTag = ImmutableByteArray.copyOfRange(boardMemberArgonHash, 0, 32);
		final ImmutableByteArray boardMemberHashSalt = ImmutableByteArray.copyOfRange(boardMemberArgonHash, 32, 48);
		final ImmutableByteArray boardMemberHash = argon2.getArgon2id(toBytes(boardMemberPassword.get()), boardMemberHashSalt);

		// Wipe the password after usage.
		boardMemberPassword.clear();

		return Objects.equals(boardMemberHashTag, boardMemberHash);
	}

	/**
	 * Hashes a password using the Argon2id algorithm.
	 *
	 * @param password the password of a board member. Must be non-null.
	 * @return the hash of the board member's password.
	 */
	private ImmutableByteArray hashPassword(final char[] password) {
		checkNotNull(password);
		final Argon2Hash hash = argon2.genArgon2id(toBytes(password));
		return ImmutableByteArray.concat(hash.tag(), hash.salt());
	}

	private ImmutableByteArray toBytes(final char[] chars) {
		final CharBuffer charBuffer = CharBuffer.wrap(chars);
		final ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
		final byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
				byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return new ImmutableByteArray(bytes);
	}
}
