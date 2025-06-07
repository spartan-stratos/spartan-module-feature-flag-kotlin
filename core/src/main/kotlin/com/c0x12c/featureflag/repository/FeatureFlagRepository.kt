package com.c0x12c.featureflag.repository

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.entity.FeatureFlagEntity
import com.c0x12c.featureflag.jackson.CoreJackson
import com.c0x12c.featureflag.models.FeatureFlagType
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.models.PaginatedResult
import com.c0x12c.featureflag.table.FeatureFlagTable
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

class FeatureFlagRepository(
  private val database: Database
) {
  companion object {
    val objectMapper = CoreJackson.INSTANCE
  }

  fun insert(featureFlag: FeatureFlag): UUID =
    transaction(database) {
      FeatureFlagEntity
        .new {
          name = featureFlag.name
          code = featureFlag.code
          description = featureFlag.description
          enabled = featureFlag.enabled
          metadata = featureFlag.metadata?.let { objectMapper.writeValueAsString(it) }
          type = featureFlag.type
          createdAt = Instant.now()
        }.id.value
    }

  fun getById(id: UUID): FeatureFlag? =
    transaction(database) {
      FeatureFlagEntity.findById(id)?.let {
        if (it.deletedAt == null) it.toFeatureFlag() else null
      }
    }

  fun getByCode(code: String): FeatureFlag? =
    transaction(database) {
      FeatureFlagEntity
        .find {
          (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt eq null)
        }.firstOrNull()
        ?.toFeatureFlag()
    }

  fun updateEnableStatus(
    code: String,
    enabled: Boolean
  ): FeatureFlag? {
    return transaction(database) {
      val entity =
        FeatureFlagEntity
          .find {
            (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt eq null)
          }.firstOrNull() ?: return@transaction null

      entity
        .apply {
          this.enabled = enabled
          updatedAt = Instant.now()
        }.toFeatureFlag()
    }
  }

  fun update(
    code: String,
    featureFlag: FeatureFlag
  ): FeatureFlag? {
    return transaction(database) {
      val entity =
        FeatureFlagEntity
          .find {
            (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt eq null)
          }.firstOrNull() ?: return@transaction null
      entity
        .apply {
          name = featureFlag.name
          description = featureFlag.description
          enabled = featureFlag.enabled
          metadata = featureFlag.metadata?.let { objectMapper.writeValueAsString(it) }
          type = featureFlag.type
          updatedAt = Instant.now()
        }.toFeatureFlag()
    }
  }

  fun updateProperties(
    code: String,
    enabled: Boolean?,
    description: String?,
    metadata: MetadataContent?
  ): FeatureFlag? {
    return transaction(database) {
      val entity =
        FeatureFlagEntity
          .find {
            (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt eq null)
          }.firstOrNull() ?: return@transaction null
      entity
        .apply {
          enabled?.let { this.enabled = it }
          description?.let { this.description = it }
          metadata?.let { this.metadata = objectMapper.writeValueAsString(it) }
          updatedAt = Instant.now()
        }.toFeatureFlag()
    }
  }

  fun delete(code: String): FeatureFlag? {
    return transaction(database) {
      val entity =
        FeatureFlagEntity
          .find {
            (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt eq null)
          }.firstOrNull() ?: return@transaction null

      entity.deletedAt = Instant.now()
      entity.toFeatureFlag()
    }
  }

  fun list(
    limit: Int = 100,
    offset: Long = 0,
    keyword: String? = null
  ): PaginatedResult<FeatureFlag> =
    transaction(database) {
      val query =
        FeatureFlagEntity
          .find {
            (FeatureFlagTable.deletedAt eq null) and
              (
                keyword?.lowercase()?.let {
                  (FeatureFlagTable.name.lowerCase() like "%$it%") or
                    (FeatureFlagTable.description.lowerCase() like "%$it%") or
                    (FeatureFlagTable.code.lowerCase() like "%$it%")
                } ?: Op.TRUE
              )
          }

      val count = query.count()
      val items =
        query
          .orderBy(Pair(FeatureFlagTable.code, SortOrder.ASC))
          .limit(limit, offset)
          .map { it.toFeatureFlag() }

      PaginatedResult(count = count, items = items)
    }

  fun findByMetadataType(
    type: FeatureFlagType,
    limit: Int = 100,
    offset: Long = 0
  ): PaginatedResult<FeatureFlag> =
    transaction(database) {
      val query =
        FeatureFlagEntity
          .find {
            (FeatureFlagTable.type eq type) and (FeatureFlagTable.deletedAt eq null)
          }

      val count = query.count()

      val items =
        query
          .orderBy(Pair(FeatureFlagTable.code, SortOrder.ASC))
          .limit(limit, offset)
          .map { it.toFeatureFlag() }

      PaginatedResult(count = count, items = items)
    }

  private fun FeatureFlagEntity.toFeatureFlag(): FeatureFlag =
    FeatureFlag(
      id = id.value,
      name = name,
      code = code,
      description = description,
      enabled = enabled,
      type = type,
      metadata =
        metadata?.let {
          objectMapper.readValue(it, MetadataContent::class.java)
        },
      createdAt = createdAt,
      updatedAt = updatedAt,
      deletedAt = deletedAt
    )
}
