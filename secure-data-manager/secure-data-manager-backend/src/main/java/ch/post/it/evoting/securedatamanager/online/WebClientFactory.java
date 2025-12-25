/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online;

import static ch.post.it.evoting.securedatamanager.shared.Constants.UNSUCCESSFUL_RESPONSE_MESSAGE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import javax.net.ssl.KeyManagerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.service.RSocketServiceProxyFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.evotinglibraries.domain.ConversionUtils;
import ch.post.it.evoting.evotinglibraries.domain.mapper.CBORObjectMapper;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import reactor.netty.http.client.HttpClient;

@Component
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class WebClientFactory {

	private static final String COMMUNICATION_ERROR_MESSAGE = "Unable to communicate with voting server.";

	private final ObjectMapper objectMapper;
	private final String votingServerUrl;
	private final long responseTimeout;
	private final int maximumMessageSize;
	private final String keystoreLocation;
	private final String keystorePasswordLocation;
	private final String rsocketPath;
	private final int connectionTimeout;
	private final int rsocketMtu;
	private final boolean keepAlive;
	private final boolean sslEnabled;
	private final long blockTimeout;

	private RSocketServiceProxyFactory rSocketServiceProxyFactory;

	public WebClientFactory(
			final ObjectMapper objectMapper,
			final VotingServerProperties votingServerProperties) {
		this.objectMapper = objectMapper;
		this.votingServerUrl = votingServerProperties.url();
		this.responseTimeout = votingServerProperties.response().timeout();
		this.maximumMessageSize = votingServerProperties.connection().maxMessageSize();
		this.keystoreLocation = votingServerProperties.connection().clientKeystoreLocation();
		this.keystorePasswordLocation = votingServerProperties.connection().clientKeystorePasswordLocation();
		this.rsocketPath = votingServerProperties.rsocket().path();
		this.connectionTimeout = votingServerProperties.connection().timeout();
		this.rsocketMtu = votingServerProperties.rsocket().mtu();
		this.blockTimeout = votingServerProperties.rsocket().blockTimeout();
		this.keepAlive = votingServerProperties.connection().keepAlive();
		this.sslEnabled = !this.keystoreLocation.isBlank();
	}

	@PostConstruct
	void postConstruct() {
		final CBORMapper cborMapper = CBORObjectMapper.getNewInstance();

		final MediaType supportedMediaType = MediaType.APPLICATION_CBOR;

		final RSocketRequester.Builder builder = RSocketRequester.builder()
				.rsocketConnector(connector -> connector
						.fragment(rsocketMtu)
						.payloadDecoder(PayloadDecoder.ZERO_COPY))
				.rsocketStrategies(RSocketStrategies.builder()
						.encoder(new Jackson2CborEncoder(cborMapper, supportedMediaType))
						.decoder(new Jackson2CborDecoder(cborMapper, supportedMediaType))
						.routeMatcher(new PathPatternRouteMatcher())
						.build());

		HttpClient httpClient = HttpClient.create()
				.baseUrl(votingServerUrl)
				.proxyWithSystemProperties();

		if (sslEnabled) {
			httpClient = httpClient.secure(sslContextSpec -> sslContextSpec.sslContext(getSslContext()));
		}

		final RSocketRequester rSocketRequester = builder.transport(WebsocketClientTransport.create(httpClient, rsocketPath));

		this.rSocketServiceProxyFactory = RSocketServiceProxyFactory.builder(rSocketRequester)
				.blockTimeout(Duration.ofMillis(blockTimeout))
				.build();
	}

	public WebClient getWebClient() {
		return getWebClient(UNSUCCESSFUL_RESPONSE_MESSAGE);
	}

	public WebClient getWebClient(final String unsuccessfulResponseErrorMessage) {
		final ExchangeStrategies strategies = ExchangeStrategies
				.builder()
				.codecs(clientDefaultCodecsConfigurer -> {
					clientDefaultCodecsConfigurer.defaultCodecs()
							.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_NDJSON));
					clientDefaultCodecsConfigurer.defaultCodecs()
							.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_NDJSON));
					clientDefaultCodecsConfigurer.defaultCodecs().maxInMemorySize(maximumMessageSize * 1024 * 1024);
				}).build();

		HttpClient httpClient = HttpClient.create()
				.resolver(io.netty.resolver.DefaultAddressResolverGroup.INSTANCE)
				.keepAlive(keepAlive)
				.option(ChannelOption.SO_KEEPALIVE, keepAlive)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
				.doOnError((request, error) -> {
					if (error instanceof final IOException ioException) {
						throw new UncheckedIOException(COMMUNICATION_ERROR_MESSAGE, ioException);
					}
					throw new IllegalStateException(unsuccessfulResponseErrorMessage, error);
				}, (response, error) -> {
					if (error instanceof final IOException ioException) {
						throw new UncheckedIOException(COMMUNICATION_ERROR_MESSAGE, ioException);
					}
					throw new IllegalStateException(unsuccessfulResponseErrorMessage, error);
				})
				.doOnConnected(conn -> conn
						.addHandlerFirst(new ReadTimeoutHandler(responseTimeout, TimeUnit.SECONDS))
						.addHandlerFirst(new WriteTimeoutHandler(responseTimeout, TimeUnit.SECONDS))
				)
				.responseTimeout(Duration.ofSeconds(responseTimeout))
				.proxyWithSystemProperties();

		if (sslEnabled) {
			httpClient = httpClient.secure(sslContextSpec -> sslContextSpec.sslContext(getSslContext()));
		}

		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.exchangeStrategies(strategies)
				.baseUrl(votingServerUrl).build();
	}

	public <T> T createRSocketClient(final Class<T> stubClazz) {
		return this.rSocketServiceProxyFactory.createClient(stubClazz);
	}

	private SslContext getSslContext() {
		try {
			final ImmutableByteArray fileContent = new ImmutableByteArray(Files.readAllBytes(Paths.get(keystorePasswordLocation)));
			final char[] keystorePassword = ConversionUtils.byteArrayToCharArray(fileContent);

			final KeyStore keyStore = KeyStore.getInstance(Paths.get(keystoreLocation).toFile(), keystorePassword);

			final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keystorePassword);

			return SslContextBuilder.forClient()
					.keyManager(keyManagerFactory)
					.build();

		} catch (final KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
