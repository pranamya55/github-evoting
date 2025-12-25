/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.securedatamanager.shared.process.BoardPasswordHashService;

@Service
@ConditionalOnProperty("role.isSetup")
public class ElectoralBoardConfigService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralBoardConfigService.class);

	private final BoardPasswordHashService boardPasswordHashService;
	private final ElectoralBoardConstitutionService electoralBoardConstitutionService;

	public ElectoralBoardConfigService(
			final BoardPasswordHashService boardPasswordHashService,
			final ElectoralBoardConstitutionService electoralBoardConstitutionService) {
		this.boardPasswordHashService = boardPasswordHashService;
		this.electoralBoardConstitutionService = electoralBoardConstitutionService;
	}

	/**
	 * Constitutes the electoral board.
	 *
	 * @param electionEventId                the election event id
	 * @param electoralBoardId               the electoral board id
	 * @param electoralBoardMembersPasswords the electoral board members' passwords
	 */
	public void constitute(final String electionEventId, final String electoralBoardId,
			final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords) {
		validateUUID(electionEventId);
		validateUUID(electoralBoardId);
		checkNotNull(electoralBoardMembersPasswords);
		checkArgument(electoralBoardMembersPasswords.size() >= 2, "There must be at least two passwords.");

		LOGGER.debug("Constituting electoral board... [electionEventId: {}, electoralBoardId: {}]", electionEventId, electoralBoardId);

		// Create a safe copy of the passwords for hashing
		final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswordsCopyForHashing = electoralBoardMembersPasswords.stream()
				.map(SafePasswordHolder::copy)
				.collect(toImmutableList());
		final ImmutableList<ImmutableByteArray> hashes = boardPasswordHashService.hashPasswords(electoralBoardMembersPasswordsCopyForHashing);

		LOGGER.debug("Hashed electoral board members passwords. [electionEventId: {}, electoralBoardId: {}]", electionEventId, electoralBoardId);

		// Create a safe copy of the passwords for constitution
		final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswordsCopyForConstitution = electoralBoardMembersPasswords.stream()
				.map(SafePasswordHolder::copy)
				.collect(toImmutableList());
		electoralBoardConstitutionService.constitute(electionEventId, electoralBoardMembersPasswordsCopyForConstitution, hashes);

		// Wipe the passwords after usage
		electoralBoardMembersPasswords.forEach(SafePasswordHolder::clear);
	}

}
