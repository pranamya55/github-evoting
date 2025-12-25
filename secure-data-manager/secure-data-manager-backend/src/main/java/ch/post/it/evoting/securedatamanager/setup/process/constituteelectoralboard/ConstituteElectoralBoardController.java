/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.securedatamanager.shared.process.BoardMember;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@RequestMapping("/sdm-setup/constitute-electoral-board")
@ConditionalOnProperty("role.isSetup")
public class ConstituteElectoralBoardController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConstituteElectoralBoardController.class);

	private final ElectionEventService electionEventService;
	private final ConstituteElectoralBoardService constituteElectoralBoardService;

	public ConstituteElectoralBoardController(
			final ElectionEventService electionEventService,
			final ConstituteElectoralBoardService constituteElectoralBoardService) {
		this.electionEventService = electionEventService;
		this.constituteElectoralBoardService = constituteElectoralBoardService;
	}

	@PostMapping
	public void constitute(
			@RequestBody
			final ImmutableList<char[]> electoralBoardPasswords) {

		// Create a safe copy of the passwords.
		final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords = checkNotNull(electoralBoardPasswords).stream()
				.map(SafePasswordHolder::new)
				.collect(toImmutableList());
		checkArgument(electoralBoardMembersPasswords.size() >= 2);

		// Wipe the passwords after usage
		electoralBoardPasswords.stream().forEach(pw -> Arrays.fill(pw, '\u0000'));

		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Received request to constitute the electoral boards. [electionEventId: {}]", electionEventId);

		constituteElectoralBoardService.constitute(electionEventId, electoralBoardMembersPasswords);

		LOGGER.info("The constitution of the electoral board has been started. [electionEventId: {}]", electionEventId);
	}

	@GetMapping
	public ImmutableList<BoardMember> getElectoralBoardMembers() {
		return constituteElectoralBoardService.getElectoralBoardMembers();
	}

}
