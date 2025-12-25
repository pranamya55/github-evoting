/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Online tally test suite")
@SelectClasses({ MixDecryptProcessorCC1ITCase.class, MixDecryptProcessorCC2ITCase.class, MixDecryptProcessorCC3ITCase.class,
		MixDecryptProcessorCC4ITCase.class })
public class MixDecryptProcessorITSuite {
}
