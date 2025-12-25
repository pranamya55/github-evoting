/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;

import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@Service
public class ElectoralBoardService {

	private final ElectoralBoardRepository electoralBoardRepository;

	public ElectoralBoardService(final ElectoralBoardRepository electoralBoardRepository) {
		this.electoralBoardRepository = electoralBoardRepository;
	}

	public ElectoralBoard getElectoralBoard() {
		final ImmutableList<ElectoralBoardEntity> electoralBoardEntities = ImmutableList.from(electoralBoardRepository.findAll());

		if (electoralBoardEntities.isEmpty()) {
			return null;
		}
		if (electoralBoardEntities.size() > 1) {
			throw new IllegalStateException("More than one electoral board is available in the database.");
		}

		final ElectoralBoardEntity electoralBoardEntity = electoralBoardEntities.get(0);
		final ImmutableList<BoardMember> boardMembers = IntStream.range(0, electoralBoardEntity.getBoardMembers().size())
				.mapToObj(index -> new BoardMember(String.valueOf(index), electoralBoardEntity.getBoardMembers().get(index)))
				.collect(toImmutableList());

		return new ElectoralBoard(electoralBoardEntity.getElectoralBoardId(), electoralBoardEntity.getAlias(), boardMembers);
	}

	@Transactional
	public void updateStatus(final String electoralBoardId, final Status status) {
		final ElectoralBoardEntity electoralBoardEntity = electoralBoardRepository.findById(electoralBoardId)
				.orElseThrow(() -> new IllegalStateException("The electoral board with the given id does not exist."));
		electoralBoardEntity.setStatus(status.name());
		electoralBoardRepository.save(electoralBoardEntity);
	}

}
