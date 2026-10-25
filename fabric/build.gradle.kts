import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("net.fabricmc.fabric-loom")
  id("com.gradleup.shadow")
}

val minecraft = "26.3"
val loader = "0.20.1"
val fabricApi = "0.165.0+26.3"
val adventureVersion = "5.2.0"
val sqlite = "3.53.4.0"

// Each platform's jar carries its name, as “Platforms” in CLAUDE.md says: mobosses-fabric-X.Y.Z.jar.
base {
  archivesName = "mobosses-fabric"
}

// Core's files, the ones Mo’Bosses copies, belong to the mod in dev runs as in the jar: Mo’Bosses reads them from its
// own mod, never through the class loader, which would find another mod's `config.yml` first.
sourceSets.main {
  resources.srcDir(project(":core").file("src/main/resources"))
}

// Game tests on a real server (`./gradlew :fabric:runGameTest`), and a singleplayer world in a game window
// (`./gradlew :fabric:runClientGameTest`). They live in `src/gametest` as a mod of their own, so the release jar doesn't
// carry them; the tests themselves are modcore's, shared with NeoForge.
fabricApi {
  configureTests {
    createSourceSet = true
    modId = "mobosses_gametest"
    enableGameTests = true
    enableClientGameTests = true
    eula = true
  }
}
kotlin.sourceSets.named("gametest") {
  kotlin.srcDir(project(":modcore").file("src/gametest/kotlin"))
}

// What goes into the jar besides the mod's own classes: modcore, core, and their libraries, relocated as on Paper.
val shade: Configuration = configurations.create("shade")
val nested: Configuration = configurations.create("nested")
val adventure = listOf("minimessage", "serializer-legacy", "serializer-plain", "serializer-gson").map { "net.kyori:adventure-text-$it:$adventureVersion" }

dependencies {
  minecraft("com.mojang:minecraft:$minecraft")
  implementation("net.fabricmc:fabric-loader:$loader")
  implementation("net.fabricmc.fabric-api:fabric-api:$fabricApi")
  // The game brings SLF4J and pins its version; Exposed and HikariCP log through the game's.
  implementation(project(":core")) { exclude(group = "org.slf4j") }
  // Without its Minecraft: the game comes from Loom here.
  implementation(project(":modcore")) { isTransitive = false }
  shade(project(":core")) { exclude(group = "org.slf4j") }
  shade(project(":modcore")) { isTransitive = false }
  adventure.forEach {
    implementation(it) { exclude(group = "com.google.code.gson") }
    shade(it) { exclude(group = "com.google.code.gson") }
  }
  // The game has no SQLite driver. It loads native code by its own package name, so it goes in whole, as a jar in the
  // jar (see shadowJar): Fabric Loader picks the newest copy when another mod carries one too.
  implementation("org.xerial:sqlite-jdbc:$sqlite")
  nested("org.xerial:sqlite-jdbc:$sqlite") { isTransitive = false }
  // Optional mods Mo’Bosses will talk to, asked only when they're loaded; they bring these classes.
  compileOnly("me.lucko:fabric-permissions-api:0.7.0")
  compileOnly("com.mairwunnx:worldapi-expansion:1.0.0")
  compileOnly("chat.allay:allay-expansion:1.3.0")
}

kotlin {
  jvmToolchain(25)
}

// Fabric Loader loads only mods from a jar in the jar, so the driver gets the descriptor Loom's `include` would give it.
abstract class NestedMod : DefaultTask() {
  @get:InputFiles
  abstract val library: ConfigurableFileCollection

  @get:Input
  abstract val descriptor: Property<String>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun write() {
    val jar = library.singleFile
    ZipOutputStream(output.get().asFile.outputStream()).use { out ->
      ZipFile(jar).use { zip ->
        for (entry in zip.entries()) {
          if (entry.name == "fabric.mod.json") continue
          out.putNextEntry(ZipEntry(entry.name))
          zip.getInputStream(entry).use { it.copyTo(out) }
          out.closeEntry()
        }
      }
      out.putNextEntry(ZipEntry("fabric.mod.json"))
      out.write(descriptor.get().toByteArray())
      out.closeEntry()
    }
  }
}

val sqliteMod = tasks.register<NestedMod>("sqliteMod") {
  library.from(nested)
  descriptor = "{\"schemaVersion\":1,\"id\":\"org_xerial_sqlite-jdbc\",\"version\":\"$sqlite\",\"name\":\"sqlite-jdbc\",\"custom\":{\"fabric-loom:generated\":true}}"
  output = layout.buildDirectory.file("sqlite-mod/sqlite-jdbc-$sqlite.jar")
}

tasks {
  processResources {
    val versions = mapOf("version" to project.version.toString(), "minecraft" to minecraft, "loader" to loader, "sqlite" to sqlite)
    inputs.properties(versions)
    filesMatching("fabric.mod.json") {
      expand(versions)
    }
    from(rootProject.file("media/icon.png")) { into("assets/mobosses") }
  }

  // Kotlin and the libraries go inside, relocated, so players need no Kotlin mod and nothing clashes with a mod that
  // ships its own copy.
  shadowJar {
    archiveClassifier = ""
    // Core's files come with its jar and with this module's resources alike.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations = listOf(shade)
    from(sqliteMod) { into("META-INF/jars") }
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

  // A game that hangs is killed, and the run fails.
  matching { it.name == "runGameTest" || it.name == "runClientGameTest" }.configureEach {
    timeout = Duration.ofMinutes(10)
  }
}
