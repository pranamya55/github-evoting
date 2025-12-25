/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class XmlSignatureApplication {

	private final XmlSignatureCommandLine xmlSignatureCommandLine;
	private final ApplicationArguments applicationArguments;

	public XmlSignatureApplication(
			final XmlSignatureCommandLine xmlSignatureCommandLine,
			final ApplicationArguments applicationArguments) {
		this.xmlSignatureCommandLine = xmlSignatureCommandLine;
		this.applicationArguments = applicationArguments;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReadyEvent() {
		xmlSignatureCommandLine.run(applicationArguments.getSourceArgs());
	}

	public static void main(final String[] args) {
		SpringApplication.run(XmlSignatureApplication.class, args);
	}
}
