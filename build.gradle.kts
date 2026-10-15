import io.papermc.hangarpublishplugin.model.Platforms

plugins {
  kotlin("jvm") version "2.4.21"
  kotlin("plugin.serialization") version "2.4.21"
  id("com.gradleup.shadow") version "9.6.1"
  id("com.modrinth.minotaur") version "2.10.0"
  id("io.papermc.hangar-publish-plugin") version "0.1.4"
}

group = "ru.mairwunnx"
// The release workflow passes the version; every other build is a dev build.
version = (findProperty("releaseVersion") as String?)?.takeIf { it.isNotBlank() } ?: "dev"

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://repo.extendedclip.com/releases/")
  maven("https://maven.enginehub.org/repo/")
  maven("https://maven.playpro.com/")
  maven("https://jitpack.io/") { content { includeGroup("com.github.GriefPrevention") } }
}

val exposedVersion = "1.5.1"

dependencies {
  compileOnly("io.papermc.paper:paper-api:26.3.build.193-stable")
  // Paper ships the SQLite driver, so it stays out of the jar.
  compileOnly("org.xerial:sqlite-jdbc:3.53.4.0")
  // Optional plugins: their classes load only when they're on the server.
  compileOnly("me.clip:placeholderapi:2.12.3")
  compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.7") { isTransitive = false }
  compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.19")
  compileOnly("net.coreprotect:coreprotect:24.1") { isTransitive = false }

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
  implementation("com.charleskorn.kaml:kaml:0.104.0")
  implementation("com.maxmind.db:maxmind-db:4.2.0")
  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
  implementation("com.zaxxer:HikariCP:7.1.0")
  implementation("org.postgresql:postgresql:42.7.14")
  // MariaDB's driver also talks to MySQL; MySQL's own is GPL.
  implementation("org.mariadb.jdbc:mariadb-java-client:3.5.10")

  testImplementation("io.papermc.paper:paper-api:26.3.build.193-stable")
  testImplementation("org.xerial:sqlite-jdbc:3.53.4.0")
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

tasks.processResources {
  val version = project.version.toString()
  inputs.property("version", version)
  filesMatching("paper-plugin.yml") {
    expand("version" to version)
  }
}

tasks.shadowJar {
  archiveClassifier = ""
  mergeServiceFiles()
  dependencies {
    // Paper already provides SLF4J and routes it into the server log.
    exclude(dependency("org.slf4j:.*"))
    exclude(dependency("org.jetbrains:annotations"))
    // Checker Framework annotations come with the PostgreSQL driver and do nothing at runtime.
    exclude(dependency("org.checkerframework:.*"))
  }
  // Relocated so other plugins that ship their own copies can't clash with ours.
  listOf(
    "kotlin", "kotlinx", "com.charleskorn.kaml", "it.krzeminski.snakeyaml", "okio", "net.thauvin", "com.maxmind",
    "org.jetbrains.exposed", "com.zaxxer.hikari", "org.postgresql", "org.mariadb.jdbc",
  ).forEach { relocate(it, "ru.mairwunnx.mobosses.libs.$it") }
  exclude("META-INF/*.kotlin_module", "META-INF/versions/*/module-info.class", "module-info.class")
}

tasks.jar { enabled = false }
tasks.assemble { dependsOn(tasks.shadowJar) }

// The release workflow writes the notes to a file; a local upload falls back to the GitHub release.
val releaseNotes = (findProperty("changelogFile") as String?)?.let { file(it).readText() }
  ?: "https://github.com/MairwunNx/mobosses/releases/tag/v${project.version}"

// `./gradlew modrinth` with MODRINTH_TOKEN set; the release workflow runs it when asked to.
modrinth {
  token = providers.environmentVariable("MODRINTH_TOKEN")
  projectId = "mobosses"
  versionNumber = project.version.toString()
  versionName = "Mo’Bosses ${project.version}"
  versionType = "release"
  uploadFile.set(tasks.shadowJar)
  gameVersions.addAll("26.3")
  loaders.addAll("paper", "purpur")
  changelog = releaseNotes
  syncBodyFrom = file("MODRINTH.md").readText()
}

tasks.modrinth {
  dependsOn(tasks.modrinthSyncBody)
}

// `./gradlew publishPluginPublicationToHangar` with HANGAR_TOKEN set; the release workflow runs it when asked to.
hangarPublish {
  publications.register("plugin") {
    version = project.version.toString()
    id = "MoBosses"
    channel = "Release"
    apiKey = providers.environmentVariable("HANGAR_TOKEN")
    changelog = releaseNotes
    platforms {
      register(Platforms.PAPER) {
        jar = tasks.shadowJar.flatMap { it.archiveFile }
        platformVersions = listOf("26.3")
      }
    }
  }
}
