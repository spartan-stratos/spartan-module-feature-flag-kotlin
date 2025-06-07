package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagError
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.exception.NotifierError
import com.c0x12c.featureflag.models.FeatureFlagType
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.models.PaginatedResult
import com.c0x12c.featureflag.notification.ChangeStatus
import com.c0x12c.featureflag.notification.SlackNotifier
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface FeatureFlagService {
  fun createFeatureFlag(featureFlag: FeatureFlag): FeatureFlag

  fun getFeatureFlagByCode(code: String): FeatureFlag

  fun enableFeatureFlag(code: String): FeatureFlag

  fun disableFeatureFlag(code: String): FeatureFlag

  fun updateFeatureFlag(
    code: String,
    featureFlag: FeatureFlag
  ): FeatureFlag

  fun updateProperties(
    code: String,
    enabled: Boolean?,
    description: String?,
    metadata: MetadataContent?
  ): FeatureFlag

  fun deleteFeatureFlag(code: String)

  fun listFeatureFlags(
    limit: Int = DEFAULT_LIMIT,
    offset: Long = DEFAULT_OFFSET,
    keyword: String? = null,
    enabled: Boolean? = null
  ): PaginatedResult<FeatureFlag>

  fun findFeatureFlagsByMetadataType(
    type: FeatureFlagType,
    limit: Int = DEFAULT_LIMIT,
    offset: Long = DEFAULT_OFFSET,
    enabled: Boolean? = null
  ): PaginatedResult<FeatureFlag>

  fun isFeatureFlagEnabled(
    code: String,
    context: Map<String, Any>
  ): Boolean

  fun getMetadataValue(
    code: String,
    key: String
  ): String?

  companion object {
    const val DEFAULT_LIMIT = 10
    const val DEFAULT_OFFSET: Long = 0L
  }
}

/**
 * Service class for managing feature flags.
 *
 * @property repository The repository for feature flag data access.
 * @property cache Optional Redis cache for feature flags.
 * @property slackNotifier Optional Slack notifier for feature flag changes.
 *
 */
class DefaultFeatureFlagService(
  private val repository: FeatureFlagRepository,
  private val cache: RedisCache? = null,
  private val slackNotifier: SlackNotifier? = null
) : FeatureFlagService {
  private companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Creates a new feature flag.
   *
   * @param featureFlag The feature flag to create.
   * @return The created feature flag.
   * @throws FeatureFlagError If the created flag cannot be retrieved.
   */
  override fun createFeatureFlag(featureFlag: FeatureFlag): FeatureFlag {
    logger.info("Creating new feature flag with code: ${featureFlag.code}")

    val createdFlagId = repository.insert(featureFlag)
    val newFlag =
      repository.getById(createdFlagId)
        ?: throw FeatureFlagError("Failed to retrieve created flag")
    cache?.set(newFlag.code, newFlag)
    slackNotifier?.send(newFlag, ChangeStatus.CREATED)

    logger.info("Successfully created the feature flag ${newFlag.code}")
    return newFlag
  }

  /**
   * Retrieves a feature flag by its code.
   *
   * @param code The code of the feature flag.
   * @return The feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  override fun getFeatureFlagByCode(code: String): FeatureFlag {
    logger.info("Retrieving feature flag with code: $code")

    cache?.get(code)?.let {
      logger.info("Found feature flag in cache with code: $code")
      return it
    }

    val featureFlag =
      repository.getByCode(code)
        ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
    cache?.set(code, featureFlag)

    logger.info("Successfully retrieved the feature flag ${featureFlag.code}")
    return featureFlag
  }

  /**
   * Enables a feature flag.
   *
   * @param code The code of the feature flag to enable.
   * @return The updated feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  override fun enableFeatureFlag(code: String): FeatureFlag {
    logger.info("Enabling feature flag with code: $code")

    val updatedFlag =
      repository.updateEnableStatus(code, true)
        ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")

    cache?.set(code, updatedFlag)
    sendNotification(updatedFlag, ChangeStatus.ENABLED)

    logger.info("Successfully enabled the feature flag ${updatedFlag.code}")
    return updatedFlag
  }

  /**
   * Disables a feature flag.
   *
   * @param code The code of the feature flag to disable.
   * @return The updated feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  override fun disableFeatureFlag(code: String): FeatureFlag {
    logger.info("Disabling feature flag with code: $code")

    val updatedFlag = repository.updateEnableStatus(code, false) ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")

    cache?.set(code, updatedFlag)
    sendNotification(updatedFlag, ChangeStatus.DISABLED)

    logger.info("Successfully disabled the feature flag ${updatedFlag.code}")
    return updatedFlag
  }

  /**
   * Updates an existing feature flag.
   *
   * @param code The code of the feature flag to update.
   * @param featureFlag The updated feature flag data.
   * @return The updated feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  override fun updateFeatureFlag(
    code: String,
    featureFlag: FeatureFlag
  ): FeatureFlag {
    logger.info("Updating feature flag with code: $code")

    val updatedFlag =
      repository.update(code, featureFlag)
        ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")

    cache?.set(code, updatedFlag)
    sendNotification(updatedFlag, ChangeStatus.UPDATED)

    logger.info("Successfully updated the feature flag ${updatedFlag.code}")
    return updatedFlag
  }

  override fun updateProperties(
    code: String,
    enabled: Boolean?,
    description: String?,
    metadata: MetadataContent?
  ): FeatureFlag {
    val updatedFlag =
      repository.updateProperties(
        code = code,
        enabled = enabled,
        description = description,
        metadata = metadata
      ) ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")

    cache?.set(code, updatedFlag)
    sendNotification(updatedFlag, ChangeStatus.UPDATED)

    return updatedFlag
  }

  /**
   * Deletes a feature flag.
   *
   * @param code The code of the feature flag to delete.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  override fun deleteFeatureFlag(code: String) {
    logger.info("Deleting feature flag with code: $code")

    val result =
      repository.delete(code)
        ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")

    cache?.delete(code)
    sendNotification(result, ChangeStatus.DELETED)

    logger.info("Successfully deleted the feature flag ${result.code}")
  }

  /**
   * Lists feature flags with pagination.
   *
   * @param limit Maximum number of flags to return.
   * @param offset Number of flags to skip.
   * @return List of feature flags.
   */
  override fun listFeatureFlags(
    limit: Int,
    offset: Long,
    keyword: String?,
    enabled: Boolean?
  ): PaginatedResult<FeatureFlag> = repository.list(limit, offset, keyword, enabled)

  /**
   * Finds feature flags by metadata type.
   *
   * @param type The metadata type to search for.
   * @param limit Maximum number of flags to return.
   * @param offset Number of flags to skip.
   * @return A paginated result containing feature flags matching the metadata type.
   */
  override fun findFeatureFlagsByMetadataType(
    type: FeatureFlagType,
    limit: Int,
    offset: Long,
    enabled: Boolean?
  ): PaginatedResult<FeatureFlag> = repository.findByMetadataType(type, limit, offset, enabled)

  /**
   * Checks if a feature flag is enabled for the given context.
   *
   * @param code The code of the feature flag.
   * @param context The context for evaluating the feature flag.
   * @return True if the feature flag is enabled, false otherwise.
   */
  override fun isFeatureFlagEnabled(
    code: String,
    context: Map<String, Any>
  ): Boolean {
    val flag = getFeatureFlagByCode(code)

    // If the flag is not enabled, return false immediately
    if (!flag.enabled) return false

    return flag.metadata?.isEnabled(context) ?: true
  }

  override fun getMetadataValue(
    code: String,
    key: String
  ): String? {
    val result = getFeatureFlagByCode(code)
    return result.metadata?.extractMetadata(key)
  }

  private fun sendNotification(
    featureFlag: FeatureFlag,
    changeStatus: ChangeStatus
  ) {
    try {
      slackNotifier?.send(featureFlag, changeStatus)
    } catch (e: Exception) {
      throw NotifierError("Failed to send notification for feature flag ${featureFlag.code}", e)
    }
  }
}
