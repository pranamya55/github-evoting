/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class SetupComponentLVCCAllowListPayloadGenerator {

	private static final Hash hash = createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Base64 base64 = createBase64();

	public SetupComponentLVCCAllowListPayload generate() {
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();

		return generate(electionEventId, verificationCardSetId);
	}

	public SetupComponentLVCCAllowListPayload generate(final String electionEventId, final String verificationCardSetId) {
		final ImmutableList<String> longVoteCastReturnCodesAllowList = ImmutableList.of(
				base64.base64Encode(ImmutableByteArray.of((byte) 5)),
				base64.base64Encode(ImmutableByteArray.of((byte) 17))
		);

		final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload = new SetupComponentLVCCAllowListPayload(electionEventId,
				verificationCardSetId, longVoteCastReturnCodesAllowList);

		final ImmutableByteArray payloadHash = hash.recursiveHash(setupComponentLVCCAllowListPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		setupComponentLVCCAllowListPayload.setSignature(signature);

		return setupComponentLVCCAllowListPayload;
	}
}