import java.time.Duration

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("net.neoforged.moddev")
  id("com.gradleup.shadow")
}

val minecraft = "26.3"
val neoforgeVersion = "26.3.0.95"
val adventureVersion = "5.2.0"
val sqlite = "3.53.4.0"

// Each platform's jar carries its name, as “Platforms” in CLAUDE.md says: mobosses-neoforge-X.Y.Z.jar.
base {
  archivesName = "mobosses-neoforge"
}

val modcore = project(":modcore")
val core = project(":core")
evaluationDependsOn(modcore.path)
evaluationDependsOn(core.path)
val modcoreMain: SourceSet = modcore.sourceSets.main.get()
val coreMain: SourceSet = core.sourceSets.main.get()

// Game tests on a real server (`./gradlew :neoforge:runGameTestServer`) and a singleplayer world in a game window
// (`./gradlew :neoforge:runClientGameTest`). They are modcore's, shared with Fabric, and run in a dev-only mod of their
// own, so the release jar doesn't carry them.
val gametest: SourceSet = sourceSets.create("gametest") {
  compileClasspath += sourceSets.main.get().output + modcoreMain.output
  runtimeClasspath += sourceSets.main.get().output + modcoreMain.output
}
kotlin.sourceSets.named("gametest") {
  kotlin.srcDir(modcore.file("src/gametest/kotlin"))
  kotlin.srcDir(modcore.file("src/gametestPlain/kotlin"))
}

neoForge {
  version = neoforgeVersion
  addModdingDependenciesTo(gametest)

  // The mod is this module, modcore, and core together, in dev runs as in the jar: they share packages, which
  // NeoForge's module layers keep to one jar each.
  mods {
    register("mobosses") {
      sourceSet(sourceSets.main.get())
      sourceSet(modcoreMain)
      sourceSet(coreMain)
    }
    register("mobosses_gametest") {
      sourceSet(gametest)
    }
  }

  // `./gradlew :neoforge:runClient` opens the game with the mod, for singleplayer and LAN worlds;
  // `./gradlew :neoforge:runServer` starts a dedicated server.
  runs {
    register("client") {
      client()
      loadedMods = listOf(mods["mobosses"])
    }
    register("server") {
      server()
      programArgument("--nogui")
      loadedMods = listOf(mods["mobosses"])
    }
    // Runs every test and stops.
    register("gameTestServer") {
      type = "gameTestServer"
      sourceSet = gametest
      gameDirectory = file("run/gametest")
      systemProperty("neoforge.enabledGameTestNamespaces", "mobosses_gametest")
    }
    // A singleplayer world in a game window, checked and closed by the test mod.
    register("clientGameTest") {
      client()
      sourceSet = gametest
      gameDirectory = file("run/clientgametest")
      systemProperty("mobosses.clientGameTest", "true")
    }
  }
}

// What goes into the jar besides the mod's own classes: modcore, core, and their libraries, relocated as on Paper.
val shade: Configuration = configurations.create("shade")
val adventure = listOf("minimessage", "serializer-legacy", "serializer-plain", "serializer-gson").map { "net.kyori:adventure-text-$it:$adventureVersion" }

dependencies {
  // The game brings SLF4J and pins its version; Exposed and HikariCP log through the game's.
  implementation(project(":core")) { exclude(group = "org.slf4j") }
  // Without its Minecraft: the game comes from NeoForge here.
  compileOnly(project(":modcore")) { isTransitive = false }
  "gametestCompileOnly"(project(":modcore")) { isTransitive = false }
  "gametestImplementation"(project(":core")) { exclude(group = "org.slf4j") }
  "gametestRuntimeOnly"("org.xerial:sqlite-jdbc:$sqlite")
  adventure.forEach { "gametestImplementation"(it) { exclude(group = "com.google.code.gson") } }
  shade(project(":core")) { exclude(group = "org.slf4j") }
  shade(project(":modcore")) { isTransitive = false }
  adventure.forEach {
    implementation(it) { exclude(group = "com.google.code.gson") }
    shade(it) { exclude(group = "com.google.code.gson") }
  }
  // The game has no SQLite driver. It loads native code by its own package name, so it goes in whole, as a jar in the
  // jar: NeoForge picks the newest copy when another mod carries one too.
  implementation("org.xerial:sqlite-jdbc:$sqlite")
  jarJar("org.xerial:sqlite-jdbc:$sqlite") { isTransitive = false }
  // Optional mods Mo’Bosses will talk to, asked only when they're loaded; they bring these classes.
  compileOnly("com.mairwunnx:worldapi-expansion:1.0.0")
  compileOnly("chat.allay:allay-expansion:1.3.0")
}

kotlin {
  jvmToolchain(25)
}

tasks {
  processResources {
    // FML wants a version number: a dev build is 0.0.0-dev, CI's 0.0.0-dev-<sha>.
    val version = project.version.toString().let { if (it.startsWith("dev")) "0.0.0-$it" else it }
    val properties = mapOf("version" to version, "neoforge" to neoforgeVersion, "minecraft" to minecraft)
    inputs.properties(properties)
    filesMatching("META-INF/neoforge.mods.toml") { expand(properties) }
    // The mod list shows the store's icon, as Fabric's does.
    from(rootProject.file("media/icon.png")) { rename { "mobosses.png" } }
  }

  // Kotlin and the libraries go inside, relocated, so players need no Kotlin mod and nothing clashes with a mod that
  // ships its own copy. The SQLite driver rides along as NeoForge's jar in jar.
  shadowJar {
    archiveClassifier = ""
    configurations = listOf(shade)
    from(named("jarJar"))
    listOf(
      "kotlin", "kotlinx", "com.charleskorn.kaml", "it.krzeminski.snakeyaml", "okio", "net.thauvin", "com.maxmind",
      "org.jetbrains.exposed", "com.zaxxer.hikari", "org.postgresql", "org.mariadb.jdbc", "net.kyori",
    ).forEach { relocate(it, "ru.mairwunnx.mobosses.libs.$it") }
    mergeServiceFiles()
    exclude("META-INF/*.kotlin_module", "META-INF/versions/*/module-info.class", "module-info.class")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC")
    dependencies {
      exclude(dependency("org.jetbrains:annotations"))
      exclude(dependency("org.checkerframework:.*"))
    }
  }

  // The jar without Kotlin wouldn't load; the shaded one is the mod.
  jar {
    archiveClassifier = "slim"
  }

  assemble {
    dependsOn(shadowJar)
  }

  named("runGameTestServer") {
    timeout = Duration.ofMinutes(10)
  }

  // The singleplayer check writes what it found; anything but `ok` fails the run.
  named("runClientGameTest") {
    timeout = Duration.ofMinutes(6)
    val result = file("run/clientgametest/mobosses-check.txt")
    doFirst { result.delete() }
    doLast {
      val text = if (result.exists()) result.readText().trim() else "the game closed before the check ran"
      if (text != "ok") throw GradleException("The singleplayer check failed:\n$text")
    }
  }
}
