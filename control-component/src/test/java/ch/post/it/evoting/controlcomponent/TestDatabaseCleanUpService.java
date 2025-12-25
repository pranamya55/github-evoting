/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.commandmessaging.CommandRepository;
import ch.post.it.evoting.controlcomponent.process.BallotBoxRepository;
import ch.post.it.evoting.controlcomponent.process.CcmjElectionKeysRepository;
import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysRepository;
import ch.post.it.evoting.controlcomponent.process.CombinedPartiallyDecryptedPCCRepository;
import ch.post.it.evoting.controlcomponent.process.ElectionContextRepository;
import ch.post.it.evoting.controlcomponent.process.ElectionEventRepository;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateRepository;
import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteRepository;
import ch.post.it.evoting.controlcomponent.process.ExtractedElectionEventHashRepository;
import ch.post.it.evoting.controlcomponent.process.HashedLVCCSharesRepository;
import ch.post.it.evoting.controlcomponent.process.LVCCAllowListEntryRepository;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryRepository;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysRepository;
import ch.post.it.evoting.controlcomponent.process.VerificationCardRepository;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetRepository;
import ch.post.it.evoting.controlcomponent.process.configuration.generateenclongcodeshares.EncryptedLongReturnCodeSharesRepository;
import ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt.MixDecryptResultRepository;
import ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt.MixnetInitialCiphertextsRepository;
import ch.post.it.evoting.controlcomponent.process.voting.confirmvote.LVCCShareRepository;
import ch.post.it.evoting.controlcomponent.process.voting.sendvote.LCCShareRepository;
import ch.post.it.evoting.controlcomponent.process.voting.sendvote.PartiallyDecryptedPCCRepository;

@Service
public class TestDatabaseCleanUpService {

	@Autowired
	private ElectionContextRepository electionContextRepository;

	@Autowired
	private ExtractedElectionEventHashRepository extractedElectionEventHashRepository;

	@Autowired
	private SetupComponentPublicKeysRepository setupComponentPublicKeysRepository;

	@Autowired
	private CcrjReturnCodesKeysRepository ccrjReturnCodesKeysRepository;

	@Autowired
	private CcmjElectionKeysRepository ccmjElectionKeysRepository;

	@Autowired
	private VerificationCardRepository verificationCardRepository;

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private VerificationCardSetRepository verificationCardSetRepository;

	@Autowired
	private PCCAllowListEntryRepository pccAllowListEntryRepository;

	@Autowired
	private EncryptedVerifiableVoteRepository encryptedVerifiableVoteRepository;

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Autowired
	private ElectionEventStateRepository electionEventStateRepository;

	@Autowired
	private LVCCShareRepository lvccShareRepository;

	@Autowired
	private LVCCAllowListEntryRepository lvccAllowListEntryRepository;

	@Autowired
	private PartiallyDecryptedPCCRepository partiallyDecryptedPCCRepository;

	@Autowired
	private HashedLVCCSharesRepository hashedLVCCSharesRepository;

	@Autowired
	private CombinedPartiallyDecryptedPCCRepository combinedPartiallyDecryptedPCCRepository;

	@Autowired
	private EncryptedLongReturnCodeSharesRepository encryptedLongReturnCodeSharesRepository;

	@Autowired
	private MixDecryptResultRepository mixDecryptResultRepository;

	@Autowired
	private MixnetInitialCiphertextsRepository mixnetInitialCiphertextsRepository;

	@Autowired
	private LCCShareRepository lccShareRepository;

	@Autowired
	private CommandRepository commandRepository;

	public void cleanUp() {
		// VERIFICATION CARD.
		encryptedVerifiableVoteRepository.deleteAll();
		hashedLVCCSharesRepository.deleteAll();
		lvccShareRepository.deleteAll();
		combinedPartiallyDecryptedPCCRepository.deleteAll();
		lccShareRepository.deleteAll();
		partiallyDecryptedPCCRepository.deleteAll();
		verificationCardRepository.deleteAll();
		// VERIFICATION CARD SET.
		encryptedLongReturnCodeSharesRepository.deleteAll();
		ballotBoxRepository.deleteAll();
		pccAllowListEntryRepository.deleteAll();
		lvccAllowListEntryRepository.deleteAll();
		verificationCardSetRepository.deleteAll();
		// BALLOT BOXES.
		mixDecryptResultRepository.deleteAll();
		mixnetInitialCiphertextsRepository.deleteAll();
		// ELECTION EVENT.
		ccrjReturnCodesKeysRepository.deleteAll();
		ccmjElectionKeysRepository.deleteAll();
		setupComponentPublicKeysRepository.deleteAll();
		extractedElectionEventHashRepository.deleteAll();
		electionContextRepository.deleteAll();
		electionEventStateRepository.deleteAll();
		electionEventRepository.deleteAll();
		// TECHNICAL.
		commandRepository.deleteAll();
	}

}
