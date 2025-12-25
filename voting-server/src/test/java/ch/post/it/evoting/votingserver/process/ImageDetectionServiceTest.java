/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

class ImageDetectionServiceTest {

	private static final ImageDetectionService imageDetectionService = new ImageDetectionService();

	@Test
	void isJpg() throws IOException {
		try (final InputStream is = this.getClass().getResourceAsStream("/process/imageDetectionServiceTest/image.jpg")) {
			assertNotNull(is);
			final ImmutableByteArray bytes = ImmutableByteArray.of(is.readAllBytes());
			assertFalse(imageDetectionService.isGif(bytes));
			assertFalse(imageDetectionService.isPng(bytes));
			assertFalse(imageDetectionService.isIco(bytes));
		}
	}

	@Test
	void isPng() throws IOException {
		try (final InputStream is = this.getClass().getResourceAsStream("/process/imageDetectionServiceTest/image.png")) {
			assertNotNull(is);
			final ImmutableByteArray bytes = ImmutableByteArray.of(is.readAllBytes());
			assertFalse(imageDetectionService.isJpg(bytes));
			assertFalse(imageDetectionService.isGif(bytes));
			assertTrue(imageDetectionService.isPng(bytes));
			assertFalse(imageDetectionService.isIco(bytes));
		}
	}

	@Test
	void isGif() throws IOException {
		try (final InputStream is = this.getClass().getResourceAsStream("/process/imageDetectionServiceTest/image.gif")) {
			assertNotNull(is);
			final ImmutableByteArray bytes = ImmutableByteArray.of(is.readAllBytes());
			assertFalse(imageDetectionService.isJpg(bytes));
			assertTrue(imageDetectionService.isGif(bytes));
			assertFalse(imageDetectionService.isPng(bytes));
			assertFalse(imageDetectionService.isIco(bytes));
		}
	}

	@Test
	void isIco() throws IOException {
		try (final InputStream is = this.getClass().getResourceAsStream("/process/imageDetectionServiceTest/image.ico")) {
			assertNotNull(is);
			final ImmutableByteArray bytes = ImmutableByteArray.of(is.readAllBytes());
			assertFalse(imageDetectionService.isJpg(bytes));
			assertFalse(imageDetectionService.isGif(bytes));
			assertFalse(imageDetectionService.isPng(bytes));
			assertTrue(imageDetectionService.isIco(bytes));
		}
	}
}
