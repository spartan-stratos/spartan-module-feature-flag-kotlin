package test

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.service.utils.RandomUtils

open class AbstractTest {
  fun createFeatureFlagEntity(
    name: String = RandomUtils.generateRandomString(),
    code: String = RandomUtils.generateRandomString(),
    description: String? = RandomUtils.generateRandomString(),
    enabled: Boolean = true,
    metadata: MetadataContent? = null
  ): FeatureFlag =
    FeatureFlag(
      name = name,
      code = code,
      description = description,
      enabled = enabled,
      metadata = metadata
    )
}
