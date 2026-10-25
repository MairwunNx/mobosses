plugins {
  kotlin("jvm") version "2.4.21" apply false
  kotlin("plugin.serialization") version "2.4.21" apply false
  id("com.gradleup.shadow") version "9.6.1" apply false
  id("com.modrinth.minotaur") version "2.10.0" apply false
  id("io.papermc.hangar-publish-plugin") version "0.1.4" apply false
  id("net.neoforged.moddev") version "2.0.148" apply false
  id("net.fabricmc.fabric-loom") version "1.19.3" apply false
}

allprojects {
  group = "ru.mairwunnx"
  // The release workflow passes the version; every other build is a dev build.
  version = (findProperty("releaseVersion") as String?)?.takeIf { it.isNotBlank() } ?: "dev"

  repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/") { content { includeGroup("me.clip") } }
    maven("https://repo.playeranalytics.net/releases/") { content { includeGroup("com.djrapitops") } }
    maven("https://maven.enginehub.org/repo/") { content { includeGroupByRegex("com\\.sk89q.*|org\\.enginehub.*") } }
    maven("https://maven.playpro.com/") { content { includeGroup("net.coreprotect") } }
    maven("https://jitpack.io/") { content { includeGroup("com.github.GriefPrevention") } }
    // Mixin's annotations for modcore, as Fabric publishes them.
    maven("https://maven.fabricmc.net/") { content { includeModule("net.fabricmc", "sponge-mixin") } }
    // Text Placeholder API and Common Protection API, for the mods.
    maven("https://maven.nucleoid.xyz/") { content { includeGroup("eu.pb4") } }
    // Flan and Open Parties and Claims, the claims mods, publish to Modrinth.
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
  }
}
