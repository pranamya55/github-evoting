/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENT_QUEUE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.google.common.base.Throwables;

import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
@SpringBootTest(properties = { "application.bootstrap.enabled=true" })
public abstract class ArtemisSupport {

	protected static final String DEAD_LETTER_QUEUE = "DLA.control-components";
	protected static final String CONTROL_COMPONENT_QUEUE_1 = CONTROL_COMPONENT_QUEUE + "1";
	protected static final String CONTROL_COMPONENT_QUEUE_2 = CONTROL_COMPONENT_QUEUE + "2";
	protected static final String CONTROL_COMPONENT_QUEUE_3 = CONTROL_COMPONENT_QUEUE + "3";
	protected static final String CONTROL_COMPONENT_QUEUE_4 = CONTROL_COMPONENT_QUEUE + "4";

	protected static final GenericContainer artemisContainer = new GenericContainer(
			new ImageFromDockerfile()
					.withFileFromPath("Dockerfile", Path.of(System.getProperty("user.dir")).resolveSibling("message-broker/Dockerfile"))
					.withFileFromPath(".", Path.of(System.getProperty("user.dir")).resolveSibling("message-broker/node0"))
					.withBuildArg("DOCKER_REGISTRY", System.getProperty("docker.registry")))
			.withCopyFileToContainer(MountableFile.forHostPath(
					Path.of(System.getProperty("user.dir")).resolveSibling("message-broker/node0/")), "/var/lib/artemis-instance/etc-override/")
			.withExposedPorts(61616, 8161)
			.withCreateContainerCmdModifier(cmd -> ((CreateContainerCmd) cmd).withHostName("message-broker-1"));

	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisSupport.class);

	@Autowired
	protected JmsTemplate jmsTemplate;

	@Autowired
	@Qualifier("multicastJmsTemplate")
	protected JmsTemplate multicastJmsTemplate;

	@Autowired
	@Qualifier("dlqListenerJmsTemplate")
	protected JmsTemplate dlqListenerJmsTemplate;
	@Autowired
	protected ContextHolder contextHolder;
	@MockitoSpyBean
	private MessageErrorHandler errorHandler;
	@Captor
	private ArgumentCaptor<Throwable> throwableCaptor;

	@BeforeAll
	static void initializeTenant(
			@Autowired
			final ContextHolder contextHolder) {
		contextHolder.setTenantId(TenantConstants.TEST_TENANT_ID);
	}

	@BeforeAll
	static void startContainer() {
		artemisContainer.start();
	}

	@DynamicPropertySource
	static void setup(final DynamicPropertyRegistry registry) {
		final String additionalOptions = "?ha=true&minLargeMessageSize=5242880";
		final String brokerUrl = String.format("tcp://%s:%d%s",
				artemisContainer.getHost(),
				artemisContainer.getMappedPort(61616),
				additionalOptions);
		registry.add("spring.artemis.broker-url=", () -> brokerUrl);
	}

	@AfterEach
	void cleanQueues() throws JMSException {
		final String[] queues = {
				CONTROL_COMPONENT_QUEUE_1,
				CONTROL_COMPONENT_QUEUE_2,
				CONTROL_COMPONENT_QUEUE_3,
				CONTROL_COMPONENT_QUEUE_4,
				VOTING_SERVER_ADDRESS };

		// Copies to avoid modifying original bean.
		final JmsTemplate jmsTemplateCopy = new JmsTemplate();
		BeanUtils.copyProperties(jmsTemplate, jmsTemplateCopy);
		jmsTemplateCopy.setReceiveTimeout(1);

		final JmsTemplate dlqListenerJmsTemplateCopy = new JmsTemplate();
		BeanUtils.copyProperties(dlqListenerJmsTemplate, dlqListenerJmsTemplateCopy);
		dlqListenerJmsTemplateCopy.setReceiveTimeout(1);

		for (final String queue : queues) {
			final Message message = jmsTemplateCopy.receive(queue);
			if (message != null) {
				throw new IllegalStateException(
						String.format("Queue not empty. [queue: %s messageType: %s]", queue, message.getStringProperty(MESSAGE_HEADER_MESSAGE_TYPE)));
			} else {
				LOGGER.debug("Queue is empty. [queue: {}]", queue);
			}
		}

		final Message dlqMessage = dlqListenerJmsTemplateCopy.receive(DEAD_LETTER_QUEUE);
		if (dlqMessage != null) {
			throw new IllegalStateException(
					String.format("DLQ not empty. [messageType: %s]", dlqMessage.getStringProperty(MESSAGE_HEADER_MESSAGE_TYPE)));
		} else {
			LOGGER.debug("DLQ is empty.");
		}
	}

	/**
	 * Asserts that the error handler has been called the expected number of times and that the error message is as expected.
	 */
	protected void assertExceptionMessage(final String expectedErrorMessage) {
		// The error handler is called the once per configured redelivery attempt. See <max-delivery-attempts> in broker.xml.
		verify(errorHandler, times(2)).handleError(throwableCaptor.capture());

		throwableCaptor.getAllValues().stream()
				.map(Throwables::getRootCause)
				.map(Throwable::getMessage)
				.forEach(m -> assertEquals(expectedErrorMessage, m));
	}

}

