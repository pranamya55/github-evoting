/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.direct.trust.SignatureKeystoreFactory;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementAlgorithm;

@Configuration
public class DisputeResolverConfiguration {

	@Bean
	ObjectMapper objectMapper() {
		return DomainObjectMapper.getNewInstance();
	}

	@Bean
	SignatureKeystore<Alias> signatureKeystoreService(final KeystoreRepository repository) throws IOException {
		return SignatureKeystoreFactory.createSignatureKeystore(repository.getKeystore(), repository.getKeystorePassword(),
				repository.getKeystoreAlias());
	}

	@Bean
	public ZeroKnowledgeProof zeroKnowledgeProof() {
		return ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
	}

	@Bean
	public Base64 base64() {
		return BaseEncodingFactory.createBase64();
	}

	@Bean
	public Hash hash() {
		return HashFactory.createHash();
	}

	@Bean
	PrimesMappingTableAlgorithms primesMappingTableAlgorithms() {
		return new PrimesMappingTableAlgorithms();
	}

	@Bean
	GetHashContextAlgorithm getHashContextAlgorithm(final Base64 base64, final Hash hash,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		return new GetHashContextAlgorithm(base64, hash, primesMappingTableAlgorithms);
	}

	@Bean
	GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm(final Base64 base64, final Hash hash) {
		return new GetHashExtractedElectionEventAlgorithm(base64, hash);
	}

	@Bean
	ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm(final Hash hash, final Base64 base64) {
		return new ConfirmVoteAgreementAlgorithm(hash, base64);
	}
}
