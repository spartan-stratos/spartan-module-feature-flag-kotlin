package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.client.SlackClient
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.notification.ChangeStatus
import com.c0x12c.featureflag.notification.SlackNotifier
import com.c0x12c.featureflag.notification.SlackNotifierConfig
import com.c0x12c.featureflag.service.cache.JedisClusterJacksonCache
import com.c0x12c.featureflag.service.utils.RandomUtils
import com.c0x12c.featureflag.service.utils.TestUtils
import com.c0x12c.featureflag.service.utils.TestUtils.jedisCluster
import com.c0x12c.featureflag.service.utils.TestUtils.repository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import retrofit2.Response
import test.AbstractTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeatureFlagJacksonIntegrationTest : AbstractTest() {
  private lateinit var mockSlackClient: SlackClient
  private lateinit var slackNotifier: SlackNotifier
  private lateinit var featureFlagService: FeatureFlagService
  private lateinit var redisCache: RedisCache

  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      TestUtils.setupDependencies()
    }

    @AfterAll
    @JvmStatic
    fun tearDown() {
      TestUtils.cleanDependencies()
    }
  }

  @BeforeEach
  fun beforeEach() {
    TestUtils.clearData()

    mockSlackClient = mockk(relaxed = true)
    coEvery {
      mockSlackClient.sendMessage(any(), any())
    } returns Response.success(Unit)

    slackNotifier =
      SlackNotifier(
        config =
          SlackNotifierConfig(
            webhookUrl = "xxx",
            requestHeaders = mapOf("clientId" to "test", "apiKey" to "1234")
          ),
        clientProvider = { mockSlackClient }
      )

    redisCache = JedisClusterJacksonCache(jedisCluster, "test")

    featureFlagService =
      DefaultFeatureFlagService(
        repository,
        redisCache,
        slackNotifier
      )
  }

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Test
  fun `should serialize and deserialize feature flag using jackson library`() {
    val name = RandomUtils.generateRandomString()
    val code = RandomUtils.generateRandomString()
    val metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0)
    val createdFlag =
      featureFlagService.createFeatureFlag(
        createFeatureFlagEntity(
          name = name,
          code = code,
          metadata = metadata
        )
      )

    assertNotNull(createdFlag)
    assertEquals(name, createdFlag.name)
    assertEquals(code, createdFlag.code)

    val retrievedFlag = featureFlagService.getFeatureFlagByCode(code)

    assertNotNull(retrievedFlag)
    assertEquals(name, retrievedFlag.name)
    assertEquals(code, retrievedFlag.code)
    assertEquals(true, retrievedFlag.enabled)
    assertEquals(metadata, retrievedFlag.metadata)

    verify {
      mockSlackClient.sendMessage(
        match {
          it.text.contains(code) && it.text.endsWith(" has been ${ChangeStatus.CREATED.name.lowercase()}")
        },
        match {
          it.containsKey("clientId") &&
            it.containsKey("apiKey") &&
            it["clientId"] == "test" &&
            it["apiKey"] == "1234"
        }
      )
    }
  }

  @Test
  fun `updateProperties should update all properties`() {
    val code = RandomUtils.generateRandomString()
    val initialFlag =
      createFeatureFlagEntity(
        code = code,
        enabled = true
      )
    featureFlagService.createFeatureFlag(initialFlag)

    // Test updating only description
    val newDescription = RandomUtils.generateRandomString()
    val newMetadata =
      MetadataContent.TimeBasedActivation(
        startTime = Instant.now(),
        endTime = Instant.now().plusSeconds(3600)
      )

    val allPropertiesUpdate =
      featureFlagService.updateProperties(
        code = code,
        enabled = false,
        description = newDescription,
        metadata = newMetadata
      )

    assertNotNull(allPropertiesUpdate)
    assertFalse(allPropertiesUpdate.enabled)
    assertEquals(newDescription, allPropertiesUpdate.description)
    assertTrue(allPropertiesUpdate.metadata is MetadataContent.TimeBasedActivation)
    assertEquals(allPropertiesUpdate.metadata, newMetadata)

    verify(exactly = 1) {
      mockSlackClient.sendMessage(
        match { it.text.contains(code) && it.text.endsWith(" has been ${ChangeStatus.UPDATED.name.lowercase()}") },
        any()
      )
    }
  }

  @Test
  fun `updateProperties should return null when feature flag does not exist`() {
    // Verify the flag still doesn't exist
    assertThrows<FeatureFlagNotFoundError> {
      featureFlagService.updateProperties(
        code = "NONEXISTENT_FLAG",
        enabled = true,
        description = "Updated description",
        metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0)
      )
    }
  }
}
