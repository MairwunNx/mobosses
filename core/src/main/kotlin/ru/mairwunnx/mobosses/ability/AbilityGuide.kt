package ru.mairwunnx.mobosses.ability

import ru.mairwunnx.mobosses.config.AbilitySettings

/**
 * Every ability as a helper explains it to players, the same on every platform: what it does, how to answer it, and its
 * numbers from `abilities.yml`. The texts follow the code in `ability/kinds` and `ability/signature`: change them with
 * it. A boss's own signature moves keep their radius and damage in code, scaled by their `strength`.
 */
object AbilityGuide {
  /**
   * An ability's numbers: its cooldown, its reach from a low level to its cap, its damage from a low level to its cap,
   * the share of health that sets it off, and the level it starts at. Null where it has none.
   */
  class Numbers(
    val cooldownSeconds: Double?,
    val radius: Double? = null,
    val radiusMax: Double? = null,
    val damage: Double? = null,
    val damageMax: Double? = null,
    val healthPercent: Double? = null,
    /** The lowest boss level that has it, for one that doesn't go by class. */
    val minLevel: Int? = null,
  )

  class Entry(
    val key: String,
    /** A boss type's own, which only its types have. */
    val signature: Boolean,
    /** What it does, in a sentence. */
    val description: String,
    /** How to answer it, in a sentence. */
    val tip: String,
    val numbers: (AbilitySettings) -> Numbers,
  )

  private fun seconds(millis: Long) = millis / 1000.0

  private fun shared(key: String, description: String, tip: String, numbers: (AbilitySettings) -> Numbers) = Entry(key, false, description, tip, numbers)

  /** A signature move: [damage] and [damageMax] in code, times its `strength`. */
  private fun signature(key: String, description: String, tip: String, radius: Double? = null, damage: Double? = null, damageMax: Double? = null) =
    Entry(key, true, description, tip) { settings ->
      val spec = settings.signatures[key] ?: AbilitySettings.Signature()
      val strength = spec.strength.coerceAtLeast(0.0)
      Numbers(seconds(spec.cooldown), radius, damage = damage?.times(strength), damageMax = damageMax?.times(strength))
    }

  val ALL: List<Entry> = listOf(
    shared("lightning", "Bolts strike every player it fights, however far, more scatter around them, and some set the ground alight.", "Spread out, and stand by a lightning rod or copper: it draws the bolts meant for you.") {
      Numbers(seconds(it.lightningCooldown), it.lightningRadius, it.lightningRadiusMax, it.lightningDamage, it.lightningDamageMax)
    },
    shared("summoning", "Calls its minions to its side, up to a cap, and some may roll into weaker bosses. The dragon and the wither send a pack to each player.", "Kill the minions as they come, or lure the boss away from them.") {
      Numbers(seconds(it.summoningCooldown))
    },
    shared("regeneration", "Below 80% health it heals over time under resistance, burns up arrows near it, and slows players next to it.", "Switch to melee while it heals: arrows near it burn up.") {
      Numbers(seconds(it.regenerationCooldown), it.regenAuraRadius)
    },
    shared("fire_ring", "Rings of fire around up to four players it fights, for a few seconds, on the ground and on water.", "Step out of the ring at once, or drink fire resistance.") {
      Numbers(seconds(it.fireRingCooldown), it.fireRingMinRadius, it.fireRingMaxRadius)
    },
    shared("web_ring", "A ring of cobwebs around the player it fights, and slowness for whoever stands inside.", "Cut the webs with a sword or shears, or wait: they vanish after a few seconds.") {
      Numbers(seconds(it.webRingCooldown), it.webRingRadius)
    },
    shared("teleport_strike", "Steps out of thin air two blocks behind the player it fights, within 15 blocks. Spiders never do.", "Keep your back to a wall: it needs solid ground behind you.") {
      Numbers(seconds(it.teleportStrikeCooldown), 15.0)
    },
    shared("meteor_shower", "Waves of burning meteors fall on the player it fights, however far. Each impact hurts and sets on fire.", "Keep moving, or step under a roof: meteors need open sky above you.") {
      Numbers(seconds(it.meteorShowerCooldown), it.meteorFireRadius, damage = it.meteorDamageBase, damageMax = it.meteorDamageMax)
    },
    shared("arrow_arc", "Rings of arrows fly out of it in every direction, and from a set level some carry harmful effects.", "Raise a shield, or fight from farther away, where the rings spread thin.") {
      Numbers(seconds(it.arrowArcCooldown), damage = it.arcDamageBase, damageMax = it.arcDamageMax)
    },
    shared("arrow_rain", "Waves of arrows fall on a square around the player it fights, following them. At high levels some carry harmful effects.", "Step under a roof, raise a shield, or sprint out of the square.") {
      Numbers(seconds(it.arrowRainCooldown), it.rainHalfSize, damage = it.rainDamageBase, damageMax = it.rainDamageMax)
    },
    shared("shockwave", "It roars, then slams the ground: players near it take damage and fly back, the closer the harder.", "When it roars, step out of its reach: the slam lands under a second later.") {
      Numbers(seconds(it.shockwaveCooldown), it.shockwaveRadius, damage = it.shockwaveDamageBase, damageMax = it.shockwaveDamageMax)
    },
    shared("frost_nova", "Cold bursts out of it: players around it freeze as in powder snow and slow down.", "Wear leather armor, which keeps the freezing off, or keep out of its reach.") {
      Numbers(seconds(it.frostNovaCooldown), it.frostNovaRadius)
    },
    shared("grapple", "Yanks the farthest player it fights and can see toward itself, an answer to shooting it from afar.", "Break its line of sight, or fight it up close.") {
      Numbers(seconds(it.grappleCooldown), it.grappleRange)
    },
    shared("obelisk", "Raises an end crystal nearby that heals it through a beam while it stays in range.", "Break the crystal: a few hits do it, and nothing else harms it.") {
      Numbers(seconds(it.obelisk.cooldown), it.obelisk.range)
    },
    shared("chain", "Chains the farthest player it fights who keeps away, reels them in, and hurts them once a second past the chain's length.", "Close in and hit it in melee: a few blows break the chain. Sprint between heaves.") {
      Numbers(seconds(it.chain.cooldown), it.chain.range, damage = it.chain.damageBase, damageMax = it.chain.damageMax)
    },
    shared("doubles", "Vanishes in smoke and comes back among copies of itself, each with one health, which vanish later.", "Hit each one once: a copy falls at once, the real boss doesn't.") {
      Numbers(seconds(it.doubles.cooldown), it.doubles.radius)
    },
    shared("blindness", "Gathers darkness, then a second later blinds every player in the fight within its reach.", "Run the moment you see the ink: out of its reach in time, you stay clear, and farther away it wears off early.") {
      Numbers(seconds(it.blindness.cooldown), it.blindness.radiusBase, it.blindness.radiusMax)
    },
    shared("enrage", "Once its health drops low, it hits harder and moves faster until it dies.", "Save your best blows and potions for its last stretch, and keep your distance once it rages.") {
      Numbers(null, healthPercent = it.enrageHealthShare * 100)
    },
    shared("phase_two", "A boss of a high enough level, once its health falls far enough, turns: for a few seconds it can't be hurt, it throws everyone back, and it casts faster from then on.", "Back off when it turns and wait out its moment of safety.") {
      Numbers(null, 6.0, healthPercent = it.phaseTwo.healthShare * 100, minLevel = it.phaseTwo.minLevel)
    },
    signature("potion_volley", "The witch throws splash potions at up to four players it fights: poison and slowness, weakness from level 25, harming from 50.", "Keep milk to wash the effects off, and keep apart.", 16.0),
    signature("fireball_barrage", "The blaze fires a burst of small fireballs at the player it fights: they burn players, not blocks.", "Drink fire resistance, or strafe sideways.", 24.0),
    signature("aimed_volley", "Archers loose a fan of 5 to 10 arrows at the player they fight.", "Raise a shield, or break their line of sight.", 32.0, 3.0, 9.0),
    signature("pounce", "Spiders leap at the player they fight and bite where they land: slowness, or poison from a cave spider.", "Sidestep the leap: the bite reaches only a few blocks from where it lands.", 16.0, 3.0, 9.0),
    signature("swap", "The enderman trades places with the player it fights and blinds them for a moment.", "Stay within four blocks of it, or out of its sight.", 24.0),
    signature("trident_storm", "The drowned hurls 2 to 5 tridents at the player it fights, and nobody can pick them up.", "Raise a shield, or break its line of sight.", 28.0, 6.0, 12.0),
    signature("fang_line", "The evoker sends a line of fangs along the ground toward the player it fights.", "Step sideways out of the line.", 20.0),
    signature("whirlwind", "It spins with its weapon out: every player close by, and every pet that bit it, takes a blow and flies back.", "Keep out of its reach when it winds up, or fight it with a bow.", 3.5, 4.0, 14.0),
    signature("wither_aura", "The wither skeleton gives off a withering cloud on the players around it.", "Back out of the cloud. Milk clears the wither.", 6.0),
    signature("sandstorm", "The husk raises a sandstorm: players around it can't see and grow hungry.", "Step out of the storm, and carry food.", 8.0),
    signature("infection", "The zombie breathes rot: hunger, weakness, and a moment of nausea for players close by.", "Strike and step back, and keep milk and food at hand.", 5.0),
    signature("wind_burst", "The breeze fires wind charges at up to three players it fights: knockback, no broken blocks.", "Fight it away from ledges and drops.", 20.0),
    signature("war_cry", "A piglin rallies its kind: it and the piglins near it get strength and speed, and zombified ones turn on its foe.", "Thin out the piglins around it first, or draw it away from them.", 12.0),
    signature("terror", "The phantom brings the night down: darkness and a moment of blindness for players around it.", "Hold your ground and wait the darkness out rather than chase it.", 12.0),
    signature("phase", "The vex fades from sight for a few seconds and speeds up.", "Watch its particles and keep your guard up: it comes back in seconds."),
    signature("stampede", "It charges at the player it fights and tramples whoever stands where it stops.", "Sidestep the charge at the last moment.", 2.8, 5.0, 16.0),
    signature("curse", "The guardian lays the elder guardian's curse on the players who can see it: mining fatigue for a long while.", "Break its line of sight. Milk clears the fatigue.", 24.0),
    signature("fuse", "The creeper's fuse catches early: it gets fast and hard to hurt for a few seconds.", "Back off until it calms, or fight it with a bow.", 12.0),
  )

  private val BY_KEY = ALL.associateBy { it.key }

  fun of(key: String): Entry? = BY_KEY[key.trim().lowercase()]
}
