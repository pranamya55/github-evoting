/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.votingserver.process.ElectionEventRepository;
import ch.post.it.evoting.votingserver.process.tally.mixdecrypt.ControlComponentBallotBoxPayloadRepository;
import ch.post.it.evoting.votingserver.process.tally.mixdecrypt.ControlComponentShufflePayloadRepository;
import ch.post.it.evoting.votingserver.process.tally.mixdecrypt.ControlComponentVotesHashPayloadRepository;

@Service
public class TestDatabaseCleanUpService {

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Autowired
	private ControlComponentShufflePayloadRepository controlComponentShufflePayloadRepository;

	@Autowired
	private ControlComponentBallotBoxPayloadRepository controlComponentBallotBoxPayloadRepository;

	@Autowired
	private ControlComponentVotesHashPayloadRepository controlComponentVotesHashPayloadRepository;

	public void cleanUp() {
		controlComponentBallotBoxPayloadRepository.deleteAll();
		controlComponentShufflePayloadRepository.deleteAll();
		controlComponentVotesHashPayloadRepository.deleteAll();
		electionEventRepository.deleteAll();
	}

}
