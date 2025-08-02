import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.2.0"
  id("com.gradleup.shadow") version "8.3.0"
  id("xyz.jpenilla.run-paper") version "2.3.1"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

group = "ru.mobosses"
version = "1.8.0"

repositories {
  mavenCentral()
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
  maven("https://oss.sonatype.org/content/groups/public")
}

dependencies {
  /* ── Spigot/Paper ───────────────────────────────────────────── */
  compileOnly("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")

  /* ── Messages ───────────────────────────────────────────────── */
  implementation("net.kyori:adventure-api:4.17.0")
  implementation("net.kyori:adventure-text-minimessage:4.17.0")
  implementation("net.kyori:adventure-platform-bukkit:4.3.2")

  /* ── Kotlin ─────────────────────────────────────────────────── */
  implementation(kotlin("stdlib-jdk8"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  implementation("com.charleskorn.kaml:kaml:0.58.0")

  /* ── Databases ──────────────────────────────────────────────── */
  implementation("org.xerial:sqlite-jdbc:3.46.0.0")
  implementation("com.mysql:mysql-connector-j:8.4.0")
  implementation("org.postgresql:postgresql:42.7.3")

  /* ── Exposed + Hikari ───────────────────────────────────────── */
  implementation("org.jetbrains.exposed:exposed-core:0.50.0")
  implementation("org.jetbrains.exposed:exposed-dao:0.50.0")
  implementation("org.jetbrains.exposed:exposed-jdbc:0.50.0")
  implementation("com.zaxxer:HikariCP:5.1.0")
}

/* ── shadowJar becomes default artifact ─────────────────────────── */
tasks.build { dependsOn("shadowJar") }

tasks.shadowJar {
  relocate("net.kyori", "ru.mobosses.shaded.kyori")
}

/* ── filter plugin.yml with project.version ─────────────────────── */
tasks.processResources {
  val props = mapOf("version" to version)
  inputs.properties(props)
  filteringCharset = "UTF-8"
  filesMatching("plugin.yml") { expand(props) }
}

/* ── compiler flags ─────────────────────────────────────────────── */
tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
  }
}

tasks { runServer { minecraftVersion("1.21.8") } }
kotlin { jvmToolchain(21) }