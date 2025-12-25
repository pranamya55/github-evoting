/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.setupvoting.ComputingStatus;

@Service
public class VerificationCardSetService {

	private final VerificationCardSetRepository verificationCardSetRepository;
	private final VerificationCardSetStateRepository verificationCardSetStateRepository;

	public VerificationCardSetService(
			final VerificationCardSetRepository verificationCardSetRepository,
			final VerificationCardSetStateRepository verificationCardSetStateRepository) {
		this.verificationCardSetRepository = verificationCardSetRepository;
		this.verificationCardSetStateRepository = verificationCardSetStateRepository;
	}

	public boolean exists(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);
		return verificationCardSetRepository.existsById(verificationCardSetId);
	}

	public ImmutableList<VerificationCardSetEntity> getVerificationCardSets(final String electionEventId) {
		validateUUID(electionEventId);
		return  ImmutableList.from(verificationCardSetRepository.findByElectionEventId(electionEventId));
	}

	public ImmutableList<VerificationCardSetEntity> getVerificationCardSets(final Status status) {
		final List<String> allIdsByStatus = verificationCardSetStateRepository.findAllIdsByStatus(status.toString());
		return ImmutableList.from(verificationCardSetRepository.findAllByVerificationCardSetIdIn(allIdsByStatus));
	}

	public ImmutableList<String> getVerificationCardSetIds(final String electionEventId) {
		validateUUID(electionEventId);
		return ImmutableList.from(verificationCardSetRepository.findAllIdsByElectionEventId(electionEventId));
	}

	public ImmutableList<String> getVerificationCardSetIds(final Status status) {
		final List<String> allIdsByStatus = verificationCardSetStateRepository.findAllIdsByStatus(status.toString());
		return ImmutableList.from(allIdsByStatus);
	}

	public VerificationCardSetEntity getVerificationCardSet(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);
		return verificationCardSetRepository.findById(verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException("The verification card set with the given id does not exist."));
	}

	@Transactional
	public void updateStatus(final String verificationCardSetId, final Status status) {
		validateUUID(verificationCardSetId);
		updateStatus(verificationCardSetId, status.name());
	}

	@Transactional
	public void updateStatus(final String verificationCardSetId, final ComputingStatus status) {
		validateUUID(verificationCardSetId);
		updateStatus(verificationCardSetId, status.name());
	}

	private void updateStatus(final String verificationCardSetId, final String status) {
		final VerificationCardSetStateEntity verificationCardSetStateEntity = verificationCardSetStateRepository.findById(verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException("The verification card set state with the given id does not exist."));
		verificationCardSetStateEntity.setStatus(status);
		verificationCardSetStateRepository.save(verificationCardSetStateEntity);
	}

}
