/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver;

import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.socket.nio.NioChannelOption;
import jdk.net.ExtendedSocketOptions;
import reactor.netty.http.server.ConnectionInformation;
import reactor.netty.http.server.logging.AccessLog;

@Component
public class NettyWebServerConfig implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

	private final boolean keepAlive;
	private final int keepIdle;
	private final int keepInterval;
	private final int keepCount;
	private final boolean accessLogEnabled;

	public NettyWebServerConfig(
			@Value("${NettyWebServer.keepAlive:true}")
			final boolean keepAlive,
			@Value("${NettyWebServer.keepIdle:20}")
			final int keepIdle,
			@Value("${NettyWebServer.keepInterval:20}")
			final int keepInterval,
			@Value("${NettyWebServer.keepCount:6}")
			final int keepCount,
			@Value("${reactor.netty.http.server.accessLogEnabled:false}")
			final boolean accessLogEnabled) {
		this.keepAlive = keepAlive;
		this.keepIdle = keepIdle;
		this.keepInterval = keepInterval;
		this.keepCount = keepCount;
		this.accessLogEnabled = accessLogEnabled;
	}

	@Override
	public void customize(final NettyReactiveWebServerFactory factory) {

		factory.addServerCustomizers(httpServer -> {
					if (Epoll.isAvailable()) {
						httpServer = httpServer.childOption(ChannelOption.SO_KEEPALIVE, keepAlive)
								.childOption(EpollChannelOption.TCP_KEEPIDLE, keepIdle)
								.childOption(EpollChannelOption.TCP_KEEPINTVL, keepInterval)
								.childOption(EpollChannelOption.TCP_KEEPCNT, keepCount);
					} else {
						httpServer = httpServer.childOption(ChannelOption.SO_KEEPALIVE, keepAlive)
								.childOption(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE), keepInterval)
								.childOption(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPINTERVAL), keepInterval)
								.childOption(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPCOUNT), keepCount);
					}

					httpServer = httpServer.accessLog(accessLogEnabled, x -> AccessLog.create(
							"{} - {} [{}] \"{}\" {} {} \"{}\" \"{}\" \"{}\" {} {} {} {} {} {}",
							getRemoteHostAddress(x.connectionInformation()),
							textOrDash(x.user()),
							formatDateTime(x.accessDateTime()),
							x.method() + " " + x.uri(),
							x.status(),
							x.contentLength(),
							textOrDash(x.requestHeader("Referer")),
							textOrDash(x.requestHeader("User-Agent")),
							textOrDash(x.requestHeader("Content-Type")),
							getRemoteHostPort(x.connectionInformation()),
							getServerHostName(x.connectionInformation()),
							getServerHostAddress(x.connectionInformation()),
							getServerHostPort(x.connectionInformation()),
							"-",
							x.duration())
					);
					return httpServer;
				}
		);
	}

	private String getRemoteHostPort(final ConnectionInformation connectionInformation) {
		if (connectionInformation != null &&
				connectionInformation.connectionRemoteAddress() instanceof final InetSocketAddress inetSocketAddress) {
			return String.valueOf(inetSocketAddress.getPort());
		}
		return "-";
	}

	private String getRemoteHostAddress(final ConnectionInformation connectionInformation) {
		if (connectionInformation != null &&
				connectionInformation.connectionRemoteAddress() instanceof final InetSocketAddress inetSocketAddress &&
				inetSocketAddress.getAddress() != null) {
			return inetSocketAddress.getAddress().getHostAddress();
		}
		return "-";
	}

	private String getServerHostAddress(final ConnectionInformation connectionInformation) {
		if (connectionInformation != null &&
				connectionInformation.connectionHostAddress() instanceof final InetSocketAddress inetSocketAddress &&
				inetSocketAddress.getAddress() != null) {
			return inetSocketAddress.getAddress().getHostAddress();
		}
		return "-";
	}

	private String getServerHostName(final ConnectionInformation connectionInformation) {
		if (connectionInformation != null) {
			return connectionInformation.hostName();
		}
		return "-";
	}

	private String getServerHostPort(final ConnectionInformation connectionInformation) {
		if (connectionInformation != null &&
				connectionInformation.connectionHostAddress() instanceof final InetSocketAddress inetSocketAddress) {
			return String.valueOf(inetSocketAddress.getPort());
		}
		return "-";
	}

	private String textOrDash(final CharSequence value) {
		return StringUtils.hasText(value) ? value.toString() : "-";
	}

	private String formatDateTime(final ZonedDateTime zonedDateTime) {
		if (zonedDateTime != null) {
			return zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
		} else {
			return "-";
		}
	}
}
