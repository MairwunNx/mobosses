pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mobosses"

// core holds everything that doesn't touch a game platform; paper is the plugin for Paper and its forks. modcore is what
// the mods will share, the game without a loader; fabric and neoforge are the mods, on a dedicated server and in
// singleplayer and LAN worlds, only frames so far.
include("core", "paper", "modcore", "fabric", "neoforge")
