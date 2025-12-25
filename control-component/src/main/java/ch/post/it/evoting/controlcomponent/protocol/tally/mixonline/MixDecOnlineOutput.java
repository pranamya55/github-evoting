/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;

/**
 * Regroups the output of the MixDecOnline algorithm.
 *
 * @param verifiableShuffle     the verifiable shuffle. Not null. Contains:
 *                              <ul>
 *                                  <li>c<sub>mix,j</sub>, the shuffled votes.</li>
 *                                  <li>&pi;<sub>mix,j</sub>, the shuffle proof.</li>
 *                              </ul>
 * @param verifiableDecryptions the verifiable decryptions. Not null. Contains:
 *                              <ul>
 *                                  <li>c<sub>dec,j</sub>, the partially decrypted votes.</li>
 *                                  <li>&pi;<sub>dec,j</sub>, the decryption proofs.</li>
 *                              </ul>
 */
public record MixDecOnlineOutput(VerifiableShuffle verifiableShuffle, VerifiableDecryptions verifiableDecryptions) {
}
