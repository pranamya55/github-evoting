/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

import org.junit.jupiter.api.BeforeAll;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

public class MapperSetUp {

	protected static ObjectMapper mapper;

	@BeforeAll
	static void setUpMapper() {
		mapper = DomainObjectMapper.getNewInstance();
	}
}
