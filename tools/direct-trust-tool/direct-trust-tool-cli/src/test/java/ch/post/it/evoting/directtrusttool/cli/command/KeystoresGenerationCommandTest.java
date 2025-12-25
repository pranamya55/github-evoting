/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.reset;
import static org.mockito.BDDMockito.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.directtrusttool.backend.process.generatekeystores.KeystorePropertiesDto;
import ch.post.it.evoting.directtrusttool.backend.process.generatekeystores.KeystoresGenerationService;
import ch.post.it.evoting.directtrusttool.cli.DirectTrustToolCliApplication;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateUtils;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

import picocli.CommandLine;

@SpringBootTest(webEnvironment = NONE, classes = DirectTrustToolCliApplication.class)
class KeystoresGenerationCommandTest {

	@MockitoBean
	KeystoresGenerationService keystoresGenerationService;

	@Autowired
	CommandLine.IFactory factory;

	@Autowired
	KeystoresGenerationCommand keyStoresGenerationCommand;

	@AfterEach
	void tearDown() {
		reset(keystoresGenerationService);
	}

	@ParameterizedTest
	@EnumSource(Alias.class)
	void testAllAliasesWorkIndividually(final Alias alias) {
		// given
		final KeystorePropertiesDto expected = KeystorePropertiesDtoBuilder.builder()
				.addAlias(alias)
				.build();

		// when
		final int exitCode = new CommandLine(keyStoresGenerationCommand, factory)
				.execute(
						"--components", aliasSetToCliString(expected.wantedComponents()),
						"--valid-until", expected.validUntil().format(DateTimeFormatter.ISO_LOCAL_DATE),
						"--country", expected.country(),
						"--state", expected.state(),
						"--locality", expected.locality(),
						"--organization", expected.organisation(),
						"--platform", expected.platform()
				);

		// then
		assertEquals(0, exitCode);
		then(keystoresGenerationService).should().generateKeystores("00000000000000000000000000000000", expected);
	}

	@Test
	void testAllAliasesWorkSimultaneously() {
		// given
		final KeystorePropertiesDto expected = KeystorePropertiesDtoBuilder.builder()
				.aliases(Set.of(Alias.values()))
				.build();

		// when
		final int exitCode = new CommandLine(keyStoresGenerationCommand, factory)
				.execute(
						"--components", aliasSetToCliString(expected.wantedComponents()),
						"--valid-until", expected.validUntil().format(DateTimeFormatter.ISO_LOCAL_DATE),
						"--country", expected.country(),
						"--state", expected.state(),
						"--locality", expected.locality(),
						"--organization", expected.organisation(),
						"--platform", expected.platform()
				);

		// then
		assertEquals(0, exitCode);
		then(keystoresGenerationService).should().generateKeystores("00000000000000000000000000000000", expected);
	}

	@Test
	void testWithoutPlatform() {
		// given
		final KeystorePropertiesDto expected = KeystorePropertiesDtoBuilder.builder()
				.aliases(Set.of(Alias.values()))
				.platform("")
				.build();

		// when
		final int exitCode = new CommandLine(keyStoresGenerationCommand, factory)
				.execute(
						"--components", aliasSetToCliString(expected.wantedComponents()),
						"--valid-until", expected.validUntil().format(DateTimeFormatter.ISO_LOCAL_DATE),
						"--country", expected.country(),
						"--state", expected.state(),
						"--locality", expected.locality(),
						"--organization", expected.organisation()
				);

		// then
		assertEquals(0, exitCode);
		then(keystoresGenerationService).should().generateKeystores("00000000000000000000000000000000", expected);
	}

	@Test
	void testWithCustomSessionNumber() {
		// given
		final KeystorePropertiesDto expected = KeystorePropertiesDtoBuilder.builder()
				.aliases(Set.of(Alias.values()))
				.build();

		// when
		final int exitCode = new CommandLine(keyStoresGenerationCommand, factory)
				.execute(
						"--session-id", "11111111111111111111111111111111",
						"--components", aliasSetToCliString(expected.wantedComponents()),
						"--valid-until", expected.validUntil().format(DateTimeFormatter.ISO_LOCAL_DATE),
						"--country", expected.country(),
						"--state", expected.state(),
						"--locality", expected.locality(),
						"--organization", expected.organisation(),
						"--platform", expected.platform()
				);

		// then
		assertEquals(0, exitCode);
		then(keystoresGenerationService).should().generateKeystores("11111111111111111111111111111111", expected);
	}

	static String aliasSetToCliString(final ImmutableSet<Alias> aliases) {
		return aliases.stream().map(Alias::get).reduce((a, b) -> a + "," + b).orElse("");
	}

	static class KeystorePropertiesDtoBuilder {
		private final LocalDate date = LocalDateUtils.now();
		private final String country = "testCountry";
		private final String state = "Aargau";
		private final String locality = "testLocality";
		private final String organization = "DT_CT_20001212_TT01";
		private Set<Alias> aliases = new HashSet<>();
		private String platform = "integration";

		private KeystorePropertiesDtoBuilder() {
			// use static method
		}

		public KeystorePropertiesDtoBuilder aliases(final Set<Alias> aliases) {
			this.aliases = aliases;
			return this;
		}

		public KeystorePropertiesDtoBuilder addAlias(final Alias alias) {
			this.aliases.add(alias);
			return this;
		}

		public KeystorePropertiesDto build() {
			return new KeystorePropertiesDto(date, country, state, locality, organization, ImmutableSet.from(aliases), platform);
		}

		public static KeystorePropertiesDtoBuilder builder() {
			return new KeystorePropertiesDtoBuilder();
		}

		public KeystorePropertiesDtoBuilder platform(final String platform) {
			this.platform = platform;
			return this;
		}
	}
}
