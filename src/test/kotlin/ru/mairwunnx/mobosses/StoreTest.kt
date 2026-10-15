package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.progress.BossCount
import ru.mairwunnx.mobosses.progress.Progress
import ru.mairwunnx.mobosses.progress.Store
import java.nio.file.Files
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreTest {
  private val score = { p: Progress -> Leveling.score(Settings.LevelCurve(), p.experience, p.kills) }
  private val player = UUID.fromString("0f3f4a1c-6d2b-4b8e-9a51-3f1e2d7c8b90")

  /** A file as 1.x left it: the same statements Exposed 0.50 ran, and a player's row. */
  @Test fun `a 1x SQLite file opens and keeps its progress`() {
    val folder = Files.createTempDirectory("mobosses").toFile()
    DriverManager.getConnection("jdbc:sqlite:${folder.resolve("bosses.db")}").use { db ->
      db.createStatement().use {
        it.execute("CREATE TABLE player_progress (uuid VARCHAR(36) NOT NULL PRIMARY KEY, boss_kills INT NOT NULL, total_experience BIGINT NOT NULL)")
        it.execute("CREATE INDEX player_progress_uuid ON player_progress (uuid)")
        it.execute("INSERT INTO player_progress VALUES ('$player', 1430, 67921)")
      }
    }

    Store(Settings.Database(), folder).use { store ->
      assertEquals(Progress(1430, 67921), store.load(player).get())
      assertEquals(Progress(1431, 68021), store.add(player, "Steve", 1, 100).get())
      assertEquals(Progress(1, 5), store.add(UUID(0, 1), "Alex", 1, 5).get())
    }
    // Opens again, as on the next start.
    Store(Settings.Database(), folder).use { assertEquals(Progress(1431, 68021), it.load(player).get()) }
    folder.deleteRecursively()
  }

  @Test fun `boards and boss counts read back`() {
    val folder = Files.createTempDirectory("mobosses").toFile()
    Store(Settings.Database(), folder).use { store ->
      store.add(UUID(0, 1), "Alex", 1, 50).get()
      store.add(UUID(0, 2), "Steve", 3, 500).get()
      store.add(UUID(0, 3), "Kai", 2, 200).get()
      assertEquals(listOf("Steve", "Kai"), store.top(2, score).get().map { it.name })
      assertEquals(2, store.rank(UUID(0, 3), score).get())
      assertEquals(4, store.rank(UUID(0, 9), score).get(), "a player with nothing is last")
      // Kills weigh in, as they do in levels: 40 kills and little experience beat 500 experience and 3 kills.
      store.add(UUID(0, 4), "Mia", 40, 10).get()
      assertEquals("Mia", store.top(1, score).get().single().name)

      store.count("zombie", "rare", killed = false)
      store.count("zombie", "rare", killed = false)
      store.count("zombie", "rare", killed = true)
      store.count("blaze", "mythic", killed = false)
      assertEquals(
        setOf(BossCount("zombie", "rare", 2, 1), BossCount("blaze", "mythic", 1, 0)),
        store.counts().get().toSet(),
      )
    }
    folder.deleteRecursively()
  }

  @Test fun `a reset wipes one player and leaves the others`() {
    val folder = Files.createTempDirectory("mobosses").toFile()
    Store(Settings.Database(), folder).use { store ->
      store.add(UUID(0, 1), "Alex", 4, 400).get()
      store.add(UUID(0, 2), "Steve", 1, 100).get()
      store.reset(UUID(0, 1)).get()
      assertEquals(Progress(), store.load(UUID(0, 1)).get())
      assertEquals(listOf("Steve"), store.top(10, score).get().map { it.name })
      assertEquals(Progress(1, 10), store.add(UUID(0, 1), "Alex", 1, 10).get(), "a reset player starts again from nothing")
    }
    folder.deleteRecursively()
  }

  @Test fun `a new SQLite file starts empty`() {
    val folder = Files.createTempDirectory("mobosses").toFile()
    Store(Settings.Database(), folder).use { assertEquals(Progress(), it.load(player).get()) }
    folder.deleteRecursively()
  }
}
