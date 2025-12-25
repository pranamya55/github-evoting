/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

@Service
public class ImageDetectionService {

	static final ImmutableByteArray JPG = ImmutableByteArray.of((byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
	static final ImmutableByteArray PNG = ImmutableByteArray.of((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
	static final ImmutableByteArray GIF = ImmutableByteArray.of((byte) 'G', (byte) 'I', (byte) 'F', (byte) '8');
	static final ImmutableByteArray ICO = ImmutableByteArray.of((byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00);

	private static boolean corresponds(final ImmutableByteArray bytes, final ImmutableByteArray magicNumbers) {
		if (bytes == null || bytes.elements().length < magicNumbers.elements().length) {
			return false;
		} else {
			for (int i = 0; i < magicNumbers.elements().length; i++) {
				if (magicNumbers.elements()[i] != bytes.elements()[i]) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isJpg(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return corresponds(bytes, JPG);
	}

	public boolean isPng(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return corresponds(bytes, PNG);
	}

	public boolean isGif(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return corresponds(bytes, GIF);
	}

	public boolean isIco(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return corresponds(bytes, ICO);
	}

	public boolean isAnImage(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return isJpg(bytes) || isPng(bytes) || isGif(bytes) || isIco(bytes);
	}

	public boolean isAnIcon(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return isIco(bytes) || isPng(bytes);
	}

	public Optional<MediaType> determineMediaType(final ImmutableByteArray image) {
		checkNotNull(image);

		final MediaType mediaType;
		if (isJpg(image)) {
			mediaType = MediaType.IMAGE_JPEG;
		} else if (isGif(image)) {
			mediaType = MediaType.IMAGE_GIF;
		} else if (isIco(image)) {
			mediaType = MediaType.valueOf("image/x-icon");
		} else if (isPng(image)) {
			mediaType = MediaType.IMAGE_PNG;
		} else {
			return Optional.empty();
		}
		return Optional.of(mediaType);
	}

}
