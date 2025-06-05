package com.c0x12c.featureflag.models

data class PaginatedResult<T>(
  val count: Long,
  val items: List<T>
)
