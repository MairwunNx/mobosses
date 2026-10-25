plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("net.neoforged.moddev")
}

val adventureVersion = "5.2.0"

// The game tests both mods run (`src/gametest`): each mod compiles them with its loader's `GameTest` annotation; here
// they compile once with `src/gametestPlain` (the annotation for a loader without its own), so they're checked against
// the game alone.
val gametest: SourceSet = sourceSets.create("gametest") {
  compileClasspath += sourceSets.main.get().output
}
kotlin.sourceSets.named("gametest") {
  kotlin.srcDir("src/gametestPlain/kotlin")
}

// The game without a loader, as both mods see it: NeoForm's Minecraft, whose names are the game's own since 26.1.
// Whatever spawns, fights, and rewards bosses the same way on Fabric and NeoForge lives here; `fabric` and `neoforge`
// hand in what their loader does differently (see `Loader`). Only a frame so far: the mods make no bosses yet.
neoForge {
  neoFormVersion = "26.3-1"
  addModdingDependenciesTo(gametest)
}

dependencies {
  // The game brings SLF4J and pins its version; Exposed and HikariCP log through the game's.
  api(project(":core")) { exclude(group = "org.slf4j") }
  // MiniMessage in messages, as on Paper, which ships Adventure itself; the mods carry it relocated. The game brings Gson.
  api("net.kyori:adventure-text-minimessage:$adventureVersion")
  api("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
  api("net.kyori:adventure-text-serializer-plain:$adventureVersion")
  api("net.kyori:adventure-text-serializer-gson:$adventureVersion") { exclude(group = "com.google.code.gson") }
  // Each mod carries the SQLite driver inside its jar; the game has none.
  compileOnly("org.xerial:sqlite-jdbc:3.53.4.0")
  // Mixins: Fabric Loader and NeoForge both bring Mixin and MixinExtras at runtime.
  compileOnly("net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7")
  compileOnly("io.github.llamalad7:mixinextras-common:0.5.5")
  // WorldAPI and Allay run as mods too and bring their expansion APIs, as on Paper.
  compileOnly("com.mairwunnx:worldapi-expansion:1.0.0")
  compileOnly("chat.allay:allay-expansion:1.3.0")
  // Optional mods: Mo’Bosses asks them only when they're loaded. Their APIs read the same on both loaders.
  compileOnly("eu.pb4:placeholder-api:3.2.0+26.3") { isTransitive = false }
  compileOnly("eu.pb4:common-protection-api:2.0.0") { isTransitive = false }
  compileOnly("maven.modrinth:flan:26.3-1.12.8.b-fabric") { isTransitive = false }
  compileOnly("maven.modrinth:open-parties-and-claims:neoforge-26.3-0.33.0") { isTransitive = false }

  testImplementation(kotlin("test"))
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencies {
  "gametestImplementation"(project(":core")) { exclude(group = "org.slf4j") }
  "gametestCompileOnly"("net.kyori:adventure-text-minimessage:$adventureVersion")
}

kotlin {
  jvmToolchain(25)
}

tasks.test {
  useJUnitPlatform()
}
