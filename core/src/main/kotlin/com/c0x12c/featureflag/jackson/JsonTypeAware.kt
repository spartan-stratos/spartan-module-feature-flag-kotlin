package com.c0x12c.featureflag.jackson

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Marker interface for enabling Jackson polymorphic type handling.
 *
 * This interface ensures that implementing classes include type information
 * in JSON serialization and deserialization using `@JsonTypeInfo`.
 *
 * @see com.fasterxml.jackson.annotation.JsonTypeInfo
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.CLASS,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
interface JsonTypeAware
