package com.c0x12c.featureflag.service.cache

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.jackson.CoreJackson
import redis.clients.jedis.JedisCluster

class JedisClusterJacksonCache(
  jedisCluster: JedisCluster,
  keyspace: String,
  ttlSeconds: Long = 3600
) : AbstractJedisClusterCache(
    jedisCluster = jedisCluster,
    keyspace = keyspace,
    ttlSeconds = ttlSeconds
  ) {
  companion object {
    val jackson = CoreJackson.INSTANCE
  }

  override fun serialize(featureFlag: FeatureFlag): String = jackson.writeValueAsString(featureFlag)

  override fun deserialize(data: String): FeatureFlag = jackson.readValue(data, FeatureFlag::class.java)
}
