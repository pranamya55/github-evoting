/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Component
public class ControlComponentsApplicationBootstrap {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentsApplicationBootstrap.class);

	private final JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

	public ControlComponentsApplicationBootstrap(final JmsListenerEndpointRegistry jmsListenerEndpointRegistry) {
		this.jmsListenerEndpointRegistry = jmsListenerEndpointRegistry;
	}

	@Bean
	boolean isApplicationBootstrapEnabled(
			@Value("${application.bootstrap.enabled:true}")
			final String enabled) {
		LOGGER.info("Application bootstrapped enabled {}", enabled);
		return Boolean.parseBoolean(enabled);
	}

	@EventListener(value = ApplicationReadyEvent.class, condition = "@isApplicationBootstrapEnabled")
	public void bootstrap() {
		jmsListenerEndpointRegistry.getListenerContainers().forEach(Lifecycle::start);
	}
}
