/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.domain.multitenancy.TenantConstants.TEST_TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;

@SpringBootTest
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
@ActiveProfiles("test")
@DisplayName("ElectionEventService")
class ElectionEventServiceIT {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private String electionEventId;
	private GqGroup encryptionGroup;

	@Autowired
	private ElectionEventService electionEventService;

	@Autowired
	private ContextHolder contextHolder;

	@MockitoSpyBean
	private ElectionEventRepository electionEventRepository;

	@BeforeEach
	void setUp() {
		contextHolder.setTenantId(TEST_TENANT_ID);

		electionEventId = uuidGenerator.generate();
		encryptionGroup = GroupTestData.getGqGroup();

		reset(electionEventRepository);
	}

	@Nested
	@DisplayName("saving")
	class SaveTest {

		@Test
		@DisplayName("with any null parameter throws NullPointerException")
		void saveNullParams() {
			assertThrows(NullPointerException.class, () -> electionEventService.save(null, encryptionGroup));
			assertThrows(NullPointerException.class, () -> electionEventService.save(electionEventId, null));
		}

		@Test
		@DisplayName("new encryption parameters saves to database")
		void save() {
			electionEventService.save(electionEventId, encryptionGroup);
			verify(electionEventRepository).save(any());
		}

	}

	@Nested
	@DisplayName("loading")
	class LoadTest {

		@Test
		@DisplayName("with null parameter throws NullPointerException")
		void loadNullParam() {
			assertThrows(NullPointerException.class, () -> electionEventService.getEncryptionGroup(null));
		}

		@Test
		@DisplayName("for the first time calls database")
		void firstTimeLoad() {
			electionEventService.save(electionEventId, encryptionGroup);

			final GqGroup loadedGroup = electionEventService.getEncryptionGroup(electionEventId);
			assertEquals(encryptionGroup, loadedGroup);

			verify(electionEventRepository).findById(electionEventId);
		}

		@Test
		@DisplayName("for the second time uses cache")
		void secondLoadUsesCache() {
			electionEventService.save(electionEventId, encryptionGroup);

			electionEventService.getEncryptionGroup(electionEventId);
			electionEventService.getEncryptionGroup(electionEventId);

			verify(electionEventRepository, times(1)).findById(electionEventId);
		}

		@Test
		@DisplayName("non existent election throws IllegalStateException")
		void nonExistentElection() {
			final String nonExistentId = uuidGenerator.generate();

			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventService.getEncryptionGroup(nonExistentId));

			final String expectedMessage = String.format("Election event not found. [electionEventId: %s]", nonExistentId);
			assertEquals(expectedMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

}
