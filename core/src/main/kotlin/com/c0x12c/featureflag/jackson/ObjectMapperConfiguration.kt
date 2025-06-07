package com.c0x12c.featureflag.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

fun configuredObjectMapper(): ObjectMapper =
  JsonMapper
    .builder()
    .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
    .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .serializationInclusion(JsonInclude.Include.NON_NULL)
    .addModule(
      KotlinModule
        .Builder()
        .withReflectionCacheSize(512)
        .configure(KotlinFeature.NullToEmptyCollection, false)
        .configure(KotlinFeature.NullToEmptyMap, false)
        .configure(KotlinFeature.NullIsSameAsDefault, false)
        .configure(KotlinFeature.SingletonSupport, false)
        .configure(KotlinFeature.StrictNullChecks, false)
        .build()
    ).addModule(JavaTimeModule())
    .build()
