/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.generators.ControlComponentCodeSharesPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;

@DisplayName("EncryptedSingleNodeLongReturnCodeSharesChunk")
class EncryptedSingleNodeLongReturnCodeSharesChunkTest {

	private static ControlComponentCodeSharesPayload controlComponentCodeSharesPayload;

	@BeforeAll
	static void setup() {
		final ControlComponentCodeSharesPayloadGenerator generator = new ControlComponentCodeSharesPayloadGenerator();
		final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads = generator.generate();
		checkState(!controlComponentCodeSharesPayloads.isEmpty());
		controlComponentCodeSharesPayload = controlComponentCodeSharesPayloads.getFirst();
	}

	@Test
	@DisplayName("with null control component code shares payload throws NullPointerException")
	void nullPayloadThrows() {
		assertThrows(NullPointerException.class, () -> new EncryptedSingleNodeLongReturnCodeSharesChunk(null));
	}

	@Test
	@DisplayName("with valid control component code shares payload does not throw")
	void validPayloadDoesNotThrows() {
		assertDoesNotThrow(() -> new EncryptedSingleNodeLongReturnCodeSharesChunk(controlComponentCodeSharesPayload));
	}

}
