import com.c0x12c.featureflag.utils.VersionUtil

plugins {
  signing

  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlinx.kover)
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.ktlint) apply false
  alias(libs.plugins.vanniktech.maven.publish)
}

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

dependencies {
  api(project(":core"))

  kover(project(":core"))
}

tasks.test {
  subprojects {
    if (name != "core") {
      tasks.withType<Test>().forEach {
        it.mustRunAfter(tasks.test)
      }
    }
  }
}

dependencies {
  kover(project(":core"))
}

allprojects {
  repositories {
    mavenLocal()
    mavenCentral()
  }
}

group = "com.c0x12c.featureflag"
version = VersionUtil.getVersionFromManifest(File(rootProject.projectDir, "manifest.json"))
print("Build version: '$version'")

subprojects {
  this.version = rootProject.version
  this.group = rootProject.group

  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  // Configure Ktlint within subprojects
  configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    debug.set(false) // Set to true to see more detailed output
    verbose.set(true) // Display more information about linting
    android.set(false) // Set to true if you're using Android projects
    outputToConsole.set(true) // Display lint results in the console
    ignoreFailures.set(false) // Set to true to allow builds to pass even if there are lint errors
    enableExperimentalRules.set(false) // Enables experimental Ktlint rules

    filter {
      exclude("**/generated/**")
      include("**/src/main/kotlin/**")
      include("**/src/test/kotlin/**")
    }

    reporters {
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN_GROUP_BY_FILE)
    }
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }
}
