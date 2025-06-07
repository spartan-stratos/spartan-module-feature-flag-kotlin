import com.vanniktech.maven.publish.SonatypeHost

plugins {
  signing
  kotlin("jvm")
  kotlin("plugin.serialization") version "1.9.24"

  alias(libs.plugins.ktlint) apply false
  alias(libs.plugins.vanniktech.maven.publish)
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.kotlin.stdlib)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.datetime)
  implementation(libs.retrofit)
  implementation(libs.retrofit.jackson)

  implementation(libs.javax.inject)

  // Database
  implementation(libs.exposed.core)
  implementation(libs.exposed.dao)
  implementation(libs.exposed.java.time)
  implementation(libs.exposed.jdbc)

  // Jackson
  implementation(libs.jackson.module.kotlin)
  implementation(libs.jackson.datatype.jsr310)
  implementation(libs.goncalossilva.murmurhash)
  implementation(libs.maven.artifact)

  // Logging
  implementation(libs.logging.logback.classic)
  implementation(libs.logging.slf4j.api)

  // Test
  testImplementation(kotlin("test"))

  // PostgreSQL
  testImplementation(libs.postgres.jdbc)

  // JUnit
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.engine)

  // MockK for mocking objects
  testImplementation(libs.mockk)

  // Coroutines for suspend functions
  testImplementation(libs.kotlinx.coroutines.test)

  // Jedis
  testImplementation(libs.redis.jedis)

  // Test Containers
  testImplementation(libs.testcontainers.junit.jupiter)
  testImplementation(libs.testcontainers.postgresql)
  testImplementation(libs.testcontainers.core)
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

  signAllPublications()

  pom {
    name.set("Feature Flag Module")
    description.set("A module for managing feature flags")
    inceptionYear.set("2025")
    url.set("https://github.com/c0x12c/spartan-module-feature-flag-kotlin/")

    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }

    developers {
      developer {
        id.set("spartan-ducduong")
        name.set("Duc Duong")
        url.set("https://github.com/spartan-ducduong/")
      }
    }

    scm {
      url.set("https://github.com/c0x12c/spartan-module-feature-flag-kotlin/")
      connection.set("scm:git:git://github.com/c0x12c/spartan-module-feature-flag-kotlin.git")
      developerConnection.set("scm:git:ssh://git@github.com/c0x12c/spartan-module-feature-flag-kotlin.git")
    }
  }
}
