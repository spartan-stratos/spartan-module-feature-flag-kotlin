package com.c0x12c.featureflag.model

data class Manifest(
  val name: String,
  val releaseStrategy: String,
  val version: String,
  val language: String
)
