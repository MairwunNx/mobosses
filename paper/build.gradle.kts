import io.papermc.hangarpublishplugin.model.Platforms

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("com.gradleup.shadow")
  id("com.modrinth.minotaur")
  id("io.papermc.hangar-publish-plugin")
}

dependencies {
  // What doesn't touch Bukkit: the formulas, the configs, the languages, the store. Shaded with its libraries.
  implementation(project(":core"))
  compileOnly("io.papermc.paper:paper-api:26.3.build.205-stable")
  // Paper ships the SQLite driver, so it stays out of the jar.
  compileOnly("org.xerial:sqlite-jdbc:3.53.4.0")
  // Optional plugins: their classes load only when they're on the server.
  compileOnly("me.clip:placeholderapi:2.12.3")
  compileOnly("com.djrapitops:plan-api:5.9-R1.3")
  compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.2.0")
  compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.7") { isTransitive = false }
  compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.20")
  compileOnly("net.coreprotect:coreprotect:24.1") { isTransitive = false }
  // WorldAPI brings these classes; Mo’Bosses only compiles against them.
  compileOnly("com.mairwunnx:worldapi-expansion:1.0.0")
  // So does Allay, the AI helper, for its expansion API.
  compileOnly("chat.allay:allay-expansion:1.3.0")

  testImplementation("io.papermc.paper:paper-api:26.3.build.205-stable")
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

tasks.processResources {
  val version = project.version.toString()
  inputs.property("version", version)
  filesMatching("paper-plugin.yml") {
    expand("version" to version)
  }
}

// Each platform's jar carries its name, as “Platforms” in CLAUDE.md says: mobosses-paper-X.Y.Z.jar.
tasks.shadowJar {
  archiveBaseName = "mobosses-paper"
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
val releaseNotes = (findProperty("changelogFile") as String?)?.let { rootProject.file(it).readText() }
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
  syncBodyFrom = rootProject.file("MODRINTH.md").readText()
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
        // The jar is over Hangar's 15 MB limit (Exposed brings kotlin-reflect), so Hangar links to the GitHub release instead.
        url = "https://github.com/MairwunNx/mobosses/releases/download/v${project.version}/mobosses-paper-${project.version}.jar"
        platformVersions = listOf("26.3")
      }
    }
  }
}
