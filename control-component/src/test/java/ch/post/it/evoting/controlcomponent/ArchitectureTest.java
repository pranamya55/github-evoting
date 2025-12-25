/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import jakarta.persistence.Entity;

import org.springframework.stereotype.Repository;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "ch.post.it.evoting.controlcomponent")
class ArchitectureTest {

	@ArchTest
	static final ArchRule NO_DEPS_FROM_CONFIGURATION_PACKAGE = noClasses().that()
			.resideInAPackage("ch.post.it.evoting.controlcomponent.protocol.configuration..")
			.and()
			.haveSimpleNameNotEndingWith("IT")
			.should().dependOnClassesThat()
			.resideInAnyPackage("ch.post.it.evoting.controlcomponent.protocol.voting..", "ch.post.it.evoting.controlcomponent.protocol.tally..");

	@ArchTest
	static final ArchRule NO_DEPS_FROM_TALLY_PACKAGE = noClasses().that()
			.resideInAPackage("ch.post.it.evoting.controlcomponent.protocol.tally..")
			.and()
			.haveSimpleNameNotEndingWith("IT")
			.should().dependOnClassesThat()
			.resideInAnyPackage("ch.post.it.evoting.controlcomponent.protocol.voting..",
					"ch.post.it.evoting.controlcomponent.protocol.configuration..");

	@ArchTest
	static final ArchRule NO_DEPS_FROM_VOTING_PACKAGE = noClasses().that()
			.resideInAPackage("ch.post.it.evoting.controlcomponent.protocol.voting..")
			.and()
			.haveSimpleNameNotEndingWith("IT")
			.should().dependOnClassesThat()
			.resideInAnyPackage("ch.post.it.evoting.controlcomponent.protocol.tally..",
					"ch.post.it.evoting.controlcomponent.protocol.configuration..");

	@ArchTest
	static final ArchRule CLASSES_IN_SENDVOTE_SHOULD_NOT_DEPEND_ON_CLASSES_IN_CONFIRMVOTE = noClasses().that()
			.resideInAPackage("ch.post.it.evoting.controlcomponent.protocol.voting.sendvote")
			.and()
			.haveSimpleNameNotEndingWith("IT")
			.should().dependOnClassesThat()
			.resideInAPackage("ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote");

	@ArchTest
	static final ArchRule CLASSES_IN_CONFIRMEVOTE_SHOULD_NOT_DEPEND_ON_CLASSES_IN_SENDVOTE = noClasses().that()
			.resideInAPackage("ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote")
			.and()
			.haveSimpleNameNotEndingWith("IT")
			.should().dependOnClassesThat()
			.resideInAPackage("ch.post.it.evoting.controlcomponent.protocol.voting.sendvote");

	@ArchTest
	static final ArchRule CLASSES_THAT_ARE_ANNOTATED_WITH_ENTITY_SHOULD_END_WITH_ENTITY = classes().that()
			.areAnnotatedWith(Entity.class)
			.should().haveSimpleNameEndingWith("Entity");

	@ArchTest
	static final ArchRule CLASSES_THAT_ARE_ANNOTATED_WITH_REPOSITORY_SHOULD_END_WITH_REPOSITORY = classes().that()
			.areAnnotatedWith(Repository.class)
			.should().haveSimpleNameEndingWith("Repository");

	@ArchTest
	static final ArchRule REPOSITORY_CLASSES_SHOULD_NOT_DEPEND_ON_OTHER_REPOSITORY_CLASSES = noClasses().that()
			.areAnnotatedWith(Repository.class)
			.should().dependOnClassesThat().areAnnotatedWith(Repository.class);
}
