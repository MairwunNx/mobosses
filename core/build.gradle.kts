plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
}

val exposedVersion = "1.6.0"
val adventureVersion = "5.2.0"

// Everything that doesn't touch a game platform: no Bukkit, no Minecraft, no loader. The formulas, the config models and
// the files the plugin copies, the languages, and the store. paper builds on it now, and the mods will through modcore.
dependencies {
  api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
  api("com.charleskorn.kaml:kaml:0.104.0")
  api("com.maxmind.db:maxmind-db:4.2.0")
  api("org.jetbrains.exposed:exposed-core:$exposedVersion")
  api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
  api("com.zaxxer:HikariCP:7.1.0")
  api("org.postgresql:postgresql:42.7.14")
  // MariaDB's driver also talks to MySQL; MySQL's own is GPL.
  api("org.mariadb.jdbc:mariadb-java-client:3.5.10")
  // Every platform brings its own SQLite driver (Paper does; the mods will carry one), so it stays out of the jar.
  compileOnly("org.xerial:sqlite-jdbc:3.53.4.0")
  // Messages are MiniMessage. Paper ships Adventure itself; the mods will carry it, relocated.
  compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
  compileOnly("net.kyori:adventure-text-serializer-legacy:$adventureVersion")

  testImplementation("org.xerial:sqlite-jdbc:3.53.4.0")
  testImplementation("net.kyori:adventure-text-minimessage:$adventureVersion")
  testImplementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
  testImplementation(kotlin("test"))
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  jvmToolchain(25)
}

tasks.test {
  useJUnitPlatform()
}

// `./gradlew balance` prints the stat curves; `--args=plugins/MoBosses` reads a server's files instead.
tasks.register<JavaExec>("balance") {
  description = "Prints boss stats, experience, and player levels for the bundled config"
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass = "ru.mairwunnx.mobosses.BalanceReportKt"
}
