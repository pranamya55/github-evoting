/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.evoting.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTableEntry;

@AnalyzeClasses(packages = "ch.post.it.evoting", importOptions = { ExcludeSpecificPackages.class })
public class SecurityRulesTests {

	@ArchTest
	static final ArchRule NO_CLASSES_SHOULD_CALL_TO_LOWER_CASE_WITHOUT_LOCALE = noClasses().should().callMethod(String.class, "toLowerCase");

	@ArchTest
	static final ArchRule NO_CLASSES_SHOULD_CALL_TO_UPPER_CASE_WITHOUT_LOCALE = noClasses().should().callMethod(String.class, "toUpperCase");

	@ArchTest
	static final ArchRule NO_CLASSES_SHOULD_CALL_LOCAL_DATE_NOW = noClasses().should().callMethod(LocalDate.class, "now");

	@ArchTest
	static final ArchRule NO_CLASSES_SHOULD_CALL_LOCAL_DATE_TIME_NOW = noClasses().should().callMethod(LocalDateTime.class, "now");

	@ArchTest
	static final ArchRule NON_STATIC_FIELDS_SHOULD_NOT_BE_PUBLIC = fields().that().areNotStatic().should().notBePublic();

	@ArchTest
	static final ArchRule TEST_CLASSES_SHOULD_USE_SETUP_ELECTION_UTILS_FOR_INSTANTIATING_PRIMES_MAPPING_TABLES = noClasses().that()
			.haveSimpleNameEndingWith("Test")
			.should().callMethod(PrimesMappingTable.class, "from", ImmutableList.class)
			.orShould().callConstructor(PrimesMappingTableEntry.class, String.class, PrimeGqElement.class, String.class, String.class);

	@ArchTest
	static final ArchRule NO_CLASSES_SHOULD_CALL_LOCAL_DATE_TIME_TO_STRING = noClasses().should().callMethod(LocalDateTime.class, "toString");
}

class ExcludeSpecificPackages implements ImportOption {
	@Override
	public boolean includes(final Location location) {
		return !location.contains("evotinglibraries") && !location.contains("cryptoprimitives");
	}
}
