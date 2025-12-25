/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;

@Configuration
public class XmlSignatureConfiguration {

	@Bean
	XMLSignatureService xmlSignatureService() {
		return new XMLSignatureService();
	}
}
