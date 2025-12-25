/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.votingserver.process.votingcardmanagement.UsedVotingCardDto;

@Repository
@Transactional(readOnly = true)
public interface VerificationCardRepository extends CrudRepository<VerificationCardEntity, String> {

	Optional<VerificationCardEntity> findByVotingCardId(final String votingCardId);

	@Query("select new ch.post.it.evoting.votingserver.process.votingcardmanagement.UsedVotingCardDto("
			+ "vc.verificationCardSetEntity.electionEventEntity.electionEventId, "
			+ "vc.verificationCardSetEntity.verificationCardSetId, "
			+ "vc.verificationCardId, "
			+ "vc.votingCardId, "
			+ "vcst.state, "
			+ "vcst.stateDate) "
			+ "from VerificationCardStateEntity vcst "
			+ "join VerificationCardEntity vc on vcst.verificationCardId = vc.verificationCardId "
			+ "where vc.verificationCardSetEntity.electionEventEntity.electionEventId like :electionEventId% "
			+ "and vcst.state <> ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState.INITIAL "
			+ "and ((cast(:usageDateTime as date) is null) or vcst.stateDate >= :usageDateTime)")
	List<UsedVotingCardDto> findAllUsedByElectionEventIdAndSinceUsageDateTime(
			@Param("electionEventId")
			final String electionEventId,
			@Param("usageDateTime")
			final LocalDateTime usageDateTime);

	Optional<VerificationCardEntity> findByCredentialId(final String credentialId);

	long countAllByVotingCardIdStartsWith(final String partialVotingCardId);

	List<VerificationCardEntity> findTop5ByVotingCardIdStartsWithOrderByVotingCardIdAsc(
			@Param("partialVotingCardId")
			final String partialVotingCardId);

}
