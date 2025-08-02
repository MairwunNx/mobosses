package ru.mobosses.entities

import java.util.UUID

data class PlayerProgress(val playerId: UUID, var bossKills: Int, var totalExperience: Long)