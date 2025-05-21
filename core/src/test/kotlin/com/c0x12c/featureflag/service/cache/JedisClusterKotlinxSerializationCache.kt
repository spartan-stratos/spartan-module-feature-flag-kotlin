package com.c0x12c.featureflag.service.cache

import com.c0x12c.featureflag.entity.FeatureFlag
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisCluster

class JedisClusterKotlinxSerializationCache(
  jedisCluster: JedisCluster,
  keyspace: String,
  ttlSeconds: Long = 3600
) : AbstractJedisClusterCache(
    jedisCluster = jedisCluster,
    keyspace = keyspace,
    ttlSeconds = ttlSeconds
  ) {
  companion object {
    val serializer = Json { ignoreUnknownKeys = true }
  }

  override fun serialize(featureFlag: FeatureFlag): String = serializer.encodeToString(featureFlag)

  override fun deserialize(data: String): FeatureFlag = serializer.decodeFromString<FeatureFlag>(data)
}
