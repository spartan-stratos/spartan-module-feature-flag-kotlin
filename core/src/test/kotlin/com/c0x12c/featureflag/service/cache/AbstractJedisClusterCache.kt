package com.c0x12c.featureflag.service.cache

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCluster

abstract class AbstractJedisClusterCache(
  private val jedisCluster: JedisCluster,
  private val keyspace: String,
  private val ttlSeconds: Long = 3600
) : RedisCache {
  abstract fun serialize(featureFlag: FeatureFlag): String

  abstract fun deserialize(data: String): FeatureFlag

  private fun keyFrom(key: String): String = "$keyspace:$key"

  override fun set(
    key: String,
    value: FeatureFlag
  ) {
    jedisCluster.setex(keyFrom(key), ttlSeconds, serialize(value))
  }

  override fun get(key: String): FeatureFlag? {
    val result = jedisCluster.get(keyFrom(key)) ?: return null
    return deserialize(result)
  }

  override fun delete(key: String) {
    jedisCluster.del(keyFrom(key)) > 0
  }

  override fun clearAll() {
    val nodes = jedisCluster.clusterNodes.keys
    nodes.forEach { node ->
      val jedisNode = Jedis(node.split(":")[0], node.split(":")[1].toInt())
      // Ensure to only clear the master nodes
      if (jedisNode.info("replication").contains("role:master")) {
        jedisNode.use { it.flushDB() }
      }
    }
  }
}
