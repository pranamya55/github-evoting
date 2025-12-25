/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.validateelectoralboard;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.process.BoardMember;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoard;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardService;
import ch.post.it.evoting.securedatamanager.tally.process.VerifyElectoralBoardPasswordService;

@RestController
@RequestMapping("/sdm-tally/validate-electoral-board")
@ConditionalOnProperty("role.isTally")
public class ValidateElectoralBoardController {

	private final ElectionEventService electionEventService;

	private final ElectoralBoardService electoralBoardService;
	private final VerifyElectoralBoardPasswordService verifyElectoralBoardPasswordService;

	public ValidateElectoralBoardController(
			final ElectionEventService electionEventService,
			final ElectoralBoardService electoralBoardService,
			final VerifyElectoralBoardPasswordService verifyElectoralBoardPasswordService) {
		this.electionEventService = electionEventService;
		this.electoralBoardService = electoralBoardService;
		this.verifyElectoralBoardPasswordService = verifyElectoralBoardPasswordService;
	}

	/**
	 * Validates an electoral board member's password against its persisted hash.
	 *
	 * @param memberIndex            the index of the member in the member list.
	 * @param electoralBoardPassword the member's password.
	 */
	@PutMapping("{memberIndex}")
	public boolean validatePassword(
			@PathVariable
			final int memberIndex,
			@RequestBody
			final char[] electoralBoardPassword) {

		// Create a safe copy of the password.
		final SafePasswordHolder electoralBoardMemberPassword = new SafePasswordHolder(checkNotNull(electoralBoardPassword));

		// Wipe the passwords after usage.
		Arrays.fill(electoralBoardPassword, '\u0000');

		final String electionEventId = electionEventService.findElectionEventId();

		try {
			return verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(electionEventId, memberIndex, electoralBoardMemberPassword);
		} catch (final InvalidPayloadSignatureException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.PAYLOAD_SIGNATURE_IS_INVALID);
		}
	}

	@GetMapping()
	public ImmutableList<BoardMember> getElectoralBoardMembers() {
		final ElectoralBoard electoralBoard = electoralBoardService.getElectoralBoard();
		return electoralBoard.members();
	}

}
