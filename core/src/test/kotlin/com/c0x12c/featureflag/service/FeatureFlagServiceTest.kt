package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.models.FeatureFlagType
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.models.PaginatedResult
import com.c0x12c.featureflag.notification.ChangeStatus
import com.c0x12c.featureflag.notification.SlackNotifier
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class FeatureFlagServiceTest {
  private lateinit var repository: FeatureFlagRepository
  private lateinit var cache: RedisCache
  private lateinit var service: FeatureFlagService
  private lateinit var slackNotifier: SlackNotifier

  @BeforeEach
  fun setup() {
    repository = mockk<FeatureFlagRepository>()
    cache = mockk<RedisCache>()
    every { cache.get(any()) } returns null
    every { cache.set(any(), any()) } just Runs

    slackNotifier = mockk<SlackNotifier>()
    every { slackNotifier.send(any(), any()) } just Runs

    service = DefaultFeatureFlagService(repository, cache, slackNotifier)
  }

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Test
  fun `createFeatureFlag should create a new feature flag`() {
    val featureFlag =
      FeatureFlag(
        name = "Test Flag",
        code = "TEST_FLAG",
        description = "A test flag",
        enabled = true,
        metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1", "user2"), percentage = 50.0)
      )

    val createdFlagId = UUID.randomUUID()
    val createdFlag = featureFlag.copy(id = createdFlagId, createdAt = Instant.now())

    every { repository.insert(any()) } returns createdFlagId
    every { repository.getById(createdFlagId) } returns createdFlag

    val result = service.createFeatureFlag(featureFlag = featureFlag)

    assertNotNull(result)
    assertEquals("Test Flag", result.name)
    assertEquals("TEST_FLAG", result.code)
    assertTrue(result.enabled)
    assertTrue(result.metadata is MetadataContent.UserTargeting)

    verify { repository.insert(featureFlag) }
    verify { repository.getById(createdFlagId) }
    verify { cache.set("TEST_FLAG", any()) }
  }

  @Test
  fun `getFeatureFlagByCode should return flag from cache if available`() {
    val code = "TEST_FLAG"
    val cachedFlag = FeatureFlag(id = UUID.randomUUID(), name = "Test Flag", code = code, description = "A test flag", enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1", "user2"), percentage = 50.0), createdAt = Instant.now())

    every { cache.get(code) } returns cachedFlag

    val result = service.getFeatureFlagByCode(code)

    assertNotNull(result)
    assertEquals("Test Flag", result.name)
    assertEquals(code, result.code)
    assertTrue(result.enabled)
    assertTrue(result.metadata is MetadataContent.UserTargeting)

    verify { cache.get(code) }
    verify(exactly = 0) { repository.getByCode(code) }
  }

  @Test
  fun `getFeatureFlagByCode should fetch from repository if not in cache`() {
    val code = "TEST_FLAG"
    val repoFlag = FeatureFlag(id = UUID.randomUUID(), name = "Test Flag", code = code, enabled = true, metadata = MetadataContent.GroupTargeting(listOf("group1", "group2"), 75.0), createdAt = Instant.now())

    every { repository.getByCode(code) } returns repoFlag

    val result = service.getFeatureFlagByCode(code)

    assertNotNull(result)
    assertEquals("Test Flag", result.name)
    assertEquals(code, result.code)
    assertTrue(result.enabled)
    assertTrue(result.metadata is MetadataContent.GroupTargeting)

    verify { cache.get(code) }
    verify { repository.getByCode(code) }
    verify { cache.set(code, any()) }
  }

  @Test
  fun `getFeatureFlagByCode should throw FeatureFlagNotFoundError if flag not found`() {
    val code = "NONEXISTENT_FLAG"

    every { repository.getByCode(code) } returns null

    assertThrows<FeatureFlagNotFoundError> {
      service.getFeatureFlagByCode(code)
    }

    verify { cache.get(code) }
    verify { repository.getByCode(code) }
  }

  @Test
  fun `updateFeatureFlag should update existing flag`() {
    val code = "TEST_FLAG"
    val updatedFlag = FeatureFlag(id = UUID.randomUUID(), name = "New Name", code = code, enabled = true, metadata = MetadataContent.TimeBasedActivation(Instant.now(), Instant.now().plusSeconds(3600)), createdAt = Instant.now(), updatedAt = Instant.now())

    every { repository.update(code, any()) } returns updatedFlag

    val result = service.updateFeatureFlag(code, updatedFlag)

    assertNotNull(result)
    assertEquals("New Name", result.name)
    assertTrue(result.enabled)
    assertTrue(result.metadata is MetadataContent.TimeBasedActivation)

    verify { repository.update(code, updatedFlag) }
    verify { cache.set(code, any()) }
  }

  @Test
  fun `updateFeatureFlag should throw FeatureFlagNotFoundError if flag not found`() {
    val code = "NONEXISTENT_FLAG"
    val updateData = FeatureFlag(name = "New Name", code = code)

    every { repository.update(code, any()) } returns null

    assertThrows<FeatureFlagNotFoundError> {
      service.updateFeatureFlag(code, updateData)
    }

    verify { repository.update(code, updateData) }
  }

  @Test
  fun `listFeatureFlags should return list of flags`() {
    val flags = listOf(FeatureFlag(id = UUID.randomUUID(), name = "Flag 1", code = "FLAG_1"), FeatureFlag(id = UUID.randomUUID(), name = "Flag 2", code = "FLAG_2"))

    every {
      repository.list(
        enabled = false,
        limit = 99,
        offset = 3
      )
    } returns PaginatedResult(count = 2, items = flags)

    val result =
      service.listFeatureFlags(
        enabled = false,
        limit = 99,
        offset = 3
      )

    assertEquals(2, result.items.size)
    assertEquals("Flag 1", result.items[0].name)
    assertEquals("FLAG_2", result.items[1].code)
  }

  @Test
  fun `listFeatureFlags with keyword should work`() {
    val flags = listOf(FeatureFlag(id = UUID.randomUUID(), name = "Flag 1", code = "FLAG_1"), FeatureFlag(id = UUID.randomUUID(), name = "Flag 2", code = "FLAG_2"))

    every {
      repository.list(
        keyword = "flag 1",
        limit = 10,
        offset = 0
      )
    } returns PaginatedResult(count = 2, listOf(flags[0]))

    val result =
      service.listFeatureFlags(
        keyword = "flag 1",
        limit = 10,
        offset = 0
      )

    assertEquals(1, result.items.size)
    assertEquals("Flag 1", result.items.first().name)
  }

  @Test
  fun `isFeatureFlagEnabled should return correct result based on metadata`() {
    val code = "TEST_FLAG"
    val flag = FeatureFlag(id = UUID.randomUUID(), name = "Test Flag", code = code, enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1", "user2"), percentage = 75.0), createdAt = Instant.now())

    every { cache.get(code) } returns flag

    assertTrue(service.isFeatureFlagEnabled(code, mapOf("userId" to "user1")))
    assertFalse(service.isFeatureFlagEnabled(code, mapOf("userId" to "user3")))

    verify(exactly = 2) { cache.get(code) }
  }

  @Test
  fun `findFeatureFlagsByMetadataType should return flags with specific metadata type`() {
    val flags = listOf(FeatureFlag(id = UUID.randomUUID(), name = "Flag 1", code = "FLAG_1", metadata = MetadataContent.UserTargeting(targetedUserIds = listOf(), percentage = 50.0)), FeatureFlag(id = UUID.randomUUID(), name = "Flag 2", code = "FLAG_2", metadata = MetadataContent.GroupTargeting(listOf(), 75.0)))

    every {
      repository.findByMetadataType(
        type = FeatureFlagType.USER_TARGETING,
        enabled = true,
        limit = 10,
        offset = 1
      )
    } returns PaginatedResult(count = 1, items = flags.subList(0, 1))

    val result =
      service.findFeatureFlagsByMetadataType(
        type = FeatureFlagType.USER_TARGETING,
        enabled = true,
        limit = 10,
        offset = 1
      )

    assertEquals(1, result.items.size)
    assertEquals("Flag 1", result.items[0].name)
    assertTrue(result.items[0].metadata is MetadataContent.UserTargeting)
  }

  @Test
  fun `enableFeatureFlag should enable an existing flag`() {
    val code = "TEST_FLAG"
    val flag = FeatureFlag(id = UUID.randomUUID(), name = "Test Flag", code = code, enabled = false, createdAt = Instant.now())
    val enabledFlag = flag.copy(enabled = true)

    every { repository.updateEnableStatus(code, true) } returns enabledFlag

    val result = service.enableFeatureFlag(code)

    assertNotNull(result)
    assertTrue(result.enabled)
    assertEquals(code, result.code)

    verify { repository.updateEnableStatus(code, true) }
    verify { cache.set(code, any()) }
  }

  @Test
  fun `enableFeatureFlag should throw FeatureFlagNotFoundError if flag not found`() {
    val code = "NONEXISTENT_FLAG"

    every { repository.updateEnableStatus(code, true) } returns null

    assertThrows<FeatureFlagNotFoundError> {
      service.enableFeatureFlag(code)
    }

    verify { repository.updateEnableStatus(code, true) }
  }

  @Test
  fun `disableFeatureFlag should disable an existing flag`() {
    val code = "TEST_FLAG"
    val flag = FeatureFlag(id = UUID.randomUUID(), name = "Test Flag", code = code, enabled = true, createdAt = Instant.now())
    val disabledFlag = flag.copy(enabled = false)

    every { repository.updateEnableStatus(code, false) } returns disabledFlag

    val result = service.disableFeatureFlag(code)

    assertNotNull(result)
    assertFalse(result.enabled)
    assertEquals(code, result.code)

    verify { repository.updateEnableStatus(code, false) }
    verify { cache.set(code, any()) }
  }

  @Test
  fun `disableFeatureFlag should throw FeatureFlagNotFoundError if flag not found`() {
    val code = "NONEXISTENT_FLAG"

    every { repository.updateEnableStatus(code, false) } returns null

    assertThrows<FeatureFlagNotFoundError> {
      service.disableFeatureFlag(code)
    }

    verify { repository.updateEnableStatus(code, false) }
  }

  @Test
  fun `deleteFeatureFlag should delete flag and send notification when flag exists`() {
    val code = "TEST_FLAG"
    val deletedFlag = FeatureFlag(id = UUID.randomUUID(), name = "Test Flag", code = code, enabled = true, createdAt = Instant.now(), deletedAt = Instant.now())

    every { repository.delete(code) } returns deletedFlag
    every { cache.delete(code) } just Runs

    assertDoesNotThrow {
      service.deleteFeatureFlag(code)
    }

    verify { repository.delete(code) }
    verify { cache.delete(code) }
    verify { slackNotifier.send(deletedFlag, ChangeStatus.DELETED) }
  }

  @Test
  fun `deleteFeatureFlag should throw FeatureFlagNotFoundError when flag does not exist`() {
    val code = "NONEXISTENT_FLAG"

    every { repository.delete(code) } returns null

    val exception =
      assertThrows<FeatureFlagNotFoundError> {
        service.deleteFeatureFlag(code)
      }

    assertEquals("Feature flag with code '$code' not found", exception.message)

    verify { repository.delete(code) }
    verify(exactly = 0) { cache.delete(any()) }
    verify(exactly = 0) { slackNotifier.send(any(), any()) }
  }

  @Test
  fun `deleteFeatureFlag should not catch other exceptions`() {
    val code = "ERROR_FLAG"

    every { repository.delete(code) } throws RuntimeException("Database error")

    assertThrows<RuntimeException> {
      service.deleteFeatureFlag(code)
    }

    verify { repository.delete(code) }
    verify(exactly = 0) { cache.delete(any()) }
    verify(exactly = 0) { slackNotifier.send(any(), any()) }
  }
}
