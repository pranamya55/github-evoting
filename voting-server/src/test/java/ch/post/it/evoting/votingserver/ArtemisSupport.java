/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver;

import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENT_QUEUE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.command.CreateContainerCmd;

import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;
import ch.post.it.evoting.votingserver.multitenancy.TenantLookupService;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ArtemisSupport {

	protected static final String CONTROL_COMPONENT_QUEUE_1 = CONTROL_COMPONENT_QUEUE + "1";
	protected static final String CONTROL_COMPONENT_QUEUE_2 = CONTROL_COMPONENT_QUEUE + "2";
	protected static final String CONTROL_COMPONENT_QUEUE_3 = CONTROL_COMPONENT_QUEUE + "3";
	protected static final String CONTROL_COMPONENT_QUEUE_4 = CONTROL_COMPONENT_QUEUE + "4";

	protected static final GenericContainer artemisContainer = new GenericContainer(
			new ImageFromDockerfile()
					.withFileFromPath("Dockerfile", Path.of(System.getProperty("user.dir")).resolveSibling("message-broker/Dockerfile"))
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

	@MockitoBean
	private TenantLookupService tenantLookupService;

	@Autowired
	private TenantService tenantService;

	@Autowired
	protected ContextHolder contextHolder;


	@PostConstruct
	public void initialize() {
		when(tenantLookupService.lookupTenantFromElectionEventId(any())).thenReturn(Optional.of(tenantService.getTenant(TenantConstants.TEST_TENANT_ID)));
	}

	@BeforeAll
	static void startContainer() {
		artemisContainer.start();
	}

	@BeforeAll
	static void initializeTenant(@Autowired
	final ContextHolder contextHolder) {
		contextHolder.setTenantId(TenantConstants.TEST_TENANT_ID);
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

		// Copy to avoid modifying original bean.
		final JmsTemplate jmsTemplateCopy = new JmsTemplate();
		BeanUtils.copyProperties(jmsTemplate, jmsTemplateCopy);
		jmsTemplateCopy.setReceiveTimeout(1);

		for (final String queue : queues) {
			final Message message = jmsTemplateCopy.receive(queue);
			if (message != null) {
				throw new IllegalStateException(
						String.format("Queue not empty. [queue: %s messageType: %s]", queue, message.getStringProperty(MESSAGE_HEADER_MESSAGE_TYPE)));
			} else {
				LOGGER.debug("Queue is empty. [queue: {}]", queue);
			}
		}
	}

}

