plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mobosses"

// paper is the plugin for Paper and its forks.
include("paper")
