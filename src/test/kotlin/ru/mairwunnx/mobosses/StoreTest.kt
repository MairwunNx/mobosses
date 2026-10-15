package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.progress.Progress
import ru.mairwunnx.mobosses.progress.Store
import java.nio.file.Files
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreTest {
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
      assertEquals(Progress(1431, 68021), store.add(player, 1, 100).get())
      assertEquals(Progress(1, 5), store.add(UUID(0, 1), 1, 5).get())
    }
    // Opens again, as on the next start.
    Store(Settings.Database(), folder).use { assertEquals(Progress(1431, 68021), it.load(player).get()) }
    folder.deleteRecursively()
  }

  @Test fun `a new SQLite file starts empty`() {
    val folder = Files.createTempDirectory("mobosses").toFile()
    Store(Settings.Database(), folder).use { assertEquals(Progress(), it.load(player).get()) }
    folder.deleteRecursively()
  }
}
