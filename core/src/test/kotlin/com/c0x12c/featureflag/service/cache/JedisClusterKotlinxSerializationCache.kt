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
  override fun serialize(featureFlag: FeatureFlag): String = Json.encodeToString(featureFlag)

  override fun deserialize(data: String): FeatureFlag = Json.decodeFromString<FeatureFlag>(data)
}
