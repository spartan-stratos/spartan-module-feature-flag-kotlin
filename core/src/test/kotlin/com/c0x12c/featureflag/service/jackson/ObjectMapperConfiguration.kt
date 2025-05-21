@file:Suppress("DEPRECATION")

package com.c0x12c.featureflag.service.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

fun ObjectMapper.configured(): ObjectMapper {
  propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
  configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
  configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
  configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
  configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  setSerializationInclusion(JsonInclude.Include.NON_NULL)
  registerModule(
    KotlinModule
      .Builder()
      .withReflectionCacheSize(512)
      .configure(KotlinFeature.NullToEmptyCollection, false)
      .configure(KotlinFeature.NullToEmptyMap, false)
      .configure(KotlinFeature.NullIsSameAsDefault, false)
      .configure(KotlinFeature.SingletonSupport, false)
      .configure(KotlinFeature.StrictNullChecks, false)
      .build()
  )
  registerModule(JavaTimeModule())
  return this
}
