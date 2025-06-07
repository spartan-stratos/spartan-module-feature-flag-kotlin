package com.c0x12c.featureflag.entity

import com.c0x12c.featureflag.models.FeatureFlagType
import com.c0x12c.featureflag.models.MetadataContent
import java.time.Instant
import java.util.UUID

data class FeatureFlag(
  val id: UUID = UUID.randomUUID(),
  val name: String,
  val code: String,
  val description: String? = null,
  val enabled: Boolean = false,
  val type: FeatureFlagType = FeatureFlagType.TOGGLE,
  val metadata: MetadataContent? = null,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant? = null,
  val deletedAt: Instant? = null
)
