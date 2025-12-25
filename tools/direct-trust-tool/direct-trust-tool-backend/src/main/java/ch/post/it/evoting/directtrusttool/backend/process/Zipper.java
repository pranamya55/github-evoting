/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;

public class Zipper {

	private Zipper() {
		// utility class
	}

	public static ImmutableByteArray zip(final ImmutableMap<String, ImmutableByteArray> filesMap) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ZipOutputStream zos = new ZipOutputStream(baos);

		try (baos; zos) {
			filesMap.forEach((key, value) -> writeFileEntry(zos, key, value));

		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		return new ImmutableByteArray(baos.toByteArray());
	}

	private static void writeFileEntry(final ZipOutputStream zos, final String key, final ImmutableByteArray value) {
		try {
			zos.putNextEntry(new ZipEntry(key));
			zos.write(value.elements());
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
