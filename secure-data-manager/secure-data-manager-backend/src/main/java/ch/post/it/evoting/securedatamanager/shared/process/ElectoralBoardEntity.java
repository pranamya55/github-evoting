/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.converters.ImmutableListConverter;
import ch.post.it.evoting.evotinglibraries.domain.validations.BoardMembersValidation;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

@Entity
@Table(name = "ELECTORAL_BOARD")
public class ElectoralBoardEntity {

	@Id
	@Column(name = "ELECTORAL_BOARD_ID")
	private String electoralBoardId;

	@Column(name = "DEFAULT_TITLE")
	private String defaultTitle;

	@Column(name = "DEFAULT_DESCRIPTION")
	private String defaultDescription;

	@Column(name = "ALIAS")
	private String alias;

	@Column(name = "BOARD_MEMBERS")
	@Convert(converter = ImmutableListConverter.class)
	private ImmutableList<String> boardMembers;

	@Column(name = "STATUS")
	private String status;

	public ElectoralBoardEntity() {
	}

	public ElectoralBoardEntity(final String electoralBoardId, final String defaultTitle, final String defaultDescription, final String alias,
			final ImmutableList<String> boardMembers, final String status) {
		Validations.validateUUID(electoralBoardId);
		Validations.validateNonBlankUCS(defaultTitle);
		Validations.validateNonBlankUCS(defaultDescription);
		Validations.validateXsToken(alias);
		BoardMembersValidation.validate(boardMembers);
		this.electoralBoardId = electoralBoardId;
		this.defaultTitle = defaultTitle;
		this.defaultDescription = defaultDescription;
		this.alias = alias;
		this.boardMembers = boardMembers;
		this.status = status;
	}

	public String getElectoralBoardId() {
		return electoralBoardId;
	}

	public String getDefaultTitle() {
		return defaultTitle;
	}

	public String getDefaultDescription() {
		return defaultDescription;
	}

	public String getAlias() {
		return alias;
	}

	public ImmutableList<String> getBoardMembers() {
		return boardMembers;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
