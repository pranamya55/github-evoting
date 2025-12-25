/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class FileCryptorApplication {

	private final EncryptionDecryptionService encryptionDecryptionService;

	public static void main(final String[] args) {
		SpringApplication.run(FileCryptorApplication.class);
	}

	public FileCryptorApplication(final EncryptionDecryptionService encryptionDecryptionService) {
		this.encryptionDecryptionService = encryptionDecryptionService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReadyEvent() {
		encryptionDecryptionService.run();
	}

}
