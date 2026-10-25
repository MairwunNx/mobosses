plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mobosses"

// core holds everything that doesn't touch a game platform; paper is the plugin for Paper and its forks.
include("core", "paper")
