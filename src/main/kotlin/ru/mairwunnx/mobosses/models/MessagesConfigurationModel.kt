@file:UseSerializers(
  SoundSerializer::class,
  ParticleSerializer::class,
  EntityTypeSerializer::class,
  ChatColorSerializer::class,
  MaterialSerializer::class,
  EnchantmentSerializer::class,
  PotionEffectTypeSerializer::class,
)

package ru.mairwunnx.mobosses.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.bukkit.Sound
import ru.mairwunnx.mobosses.serializers.ChatColorSerializer
import ru.mairwunnx.mobosses.serializers.EnchantmentSerializer
import ru.mairwunnx.mobosses.serializers.EntityTypeSerializer
import ru.mairwunnx.mobosses.serializers.MaterialSerializer
import ru.mairwunnx.mobosses.serializers.ParticleSerializer
import ru.mairwunnx.mobosses.serializers.SoundSerializer
import ru.mairwunnx.mobosses.serializers.PotionEffectTypeSerializer

@Serializable class MessagesConfigurationModel(
  @SerialName("general") val general: GeneralMessages,
  @SerialName("boss") val boss: BossMessages
) {
  @Serializable class GeneralMessages(
    @SerialName("plugin_disabled") val pluginDisabled: String, // todo: add support this in second iteration!
    @SerialName("config_reloaded") val configReloaded: String,
    @SerialName("no_permission") val noPermission: String,
    @SerialName("killall_success") val killallSuccess: String,
    @SerialName("killall_no_bosses") val killallNoBosses: String
  )

  @Serializable class BossMessages(
    @SerialName("loot") val loot: LootMessages,
    @SerialName("death") val death: DeathMessages,
    @SerialName("abilities") val abilities: AbilitiesMessages
  )

  @Serializable class LootMessages(
    @SerialName("rare_drop") val rareDrop: String,
    @SerialName("epic_drop") val epicDrop: String,
    @SerialName("legendary_drop") val legendaryDrop: String,
    @SerialName("mythic_drop") val mythicDrop: String,
    @SerialName("enabled") val enabled: Boolean
  )

  @Serializable class DeathMessages(
    @SerialName("killed") val killed: KilledMessages
  )

  @Serializable class KilledMessages(
    @SerialName("broadcast") val broadcast: BroadcastMessages,
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("sound") val sound: @Contextual Sound,
    @SerialName("sound_volume") val soundVolume: Double,
  )

  @Serializable class PersonalMessages(
    @SerialName("message") val message: String,
    @SerialName("enabled") val enabled: Boolean
  )

  @Serializable class BroadcastMessages(
    @SerialName("message") val message: String,
    @SerialName("enabled") val enabled: Boolean
  )

  @Serializable class AbilitiesMessages(
    @SerialName("lightning") val lightning: String,
    @SerialName("regeneration") val regeneration: String,
    @SerialName("summon_minions") val summonMinions: String,
    @SerialName("fire_ring") val fireRing: String,
    @SerialName("web_ring") val webRing: String,
    @SerialName("teleport_strike") val teleportStrike: String,
    @SerialName("meteor_shower") val meteorShower: String,
    @SerialName("arrow_arc") val arrowArc: String,
  )

  companion object {
    fun default() = MessagesConfigurationModel(
      general = GeneralMessages(
        pluginDisabled = "<pri>[Mo'Bosses]</pri> <warn>Плагин выключен!</warn>",
        configReloaded = "<pri>[Mo'Bosses]</pri> <ok>Конфигурация перезагружена!</ok>",
        noPermission = "<err>У вас нет прав для выполнения этой команды!</err>",
        killallSuccess = "<pri>[Mo'Bosses]</pri> <ok>Убито <num>{count}</num> боссов!</ok>",
        killallNoBosses = "<pri>[Mo'Bosses]</pri> <warn>Нет активных боссов для убийства.</warn>"
      ),
      boss = BossMessages(
        loot = LootMessages(
          rareDrop = "<pri>[Mo'Bosses]</pri> Игрок <ok><player></ok> получил <item>редкий</item> лут: <item><itemstack></item>!",
          epicDrop = "<pri>[Mo'Bosses]</pri> Игрок <ok><player></ok> получил <bold><light_purple>эпический</light_purple></bold> лут: <item><itemstack></item>!",
          legendaryDrop = "<pri>[Mo'Bosses]</pri> Игрок <ok><player></ok> получил <bold><gold>легендарный</gold></bold> лут: <item><itemstack></item>!",
          mythicDrop = "<pri>[Mo'Bosses]</pri> Игрок <ok><player></ok> получил <bold><gradient:#ff0080:#8000ff>мифический</gradient></bold> лут: <item><itemstack></item>!",
          enabled = true
        ),
        death = DeathMessages(
          killed = KilledMessages(
            broadcast = BroadcastMessages(
              message = "<pri>[Mo'Bosses]</pri> Игрок <ok><player></ok> победил босса <boss><boss_name></boss> уровня <num>Lv. <level></num>/<class> и получил +<xp><experience></xp> опыта!",
              enabled = true
            ),
            enabled = true,
            sound = Sound.UI_TOAST_CHALLENGE_COMPLETE,
            soundVolume = 0.3,
          )
        ),
        abilities = AbilitiesMessages(
          lightning = "<boss><boss_name></boss> использует <item>Молнию</item>!",
          regeneration = "<boss><boss_name></boss> начинает <hp>регенерацию</hp>!",
          summonMinions = "<boss><boss_name></boss> призывает миньонов!",
          fireRing = "<boss><boss_name></boss> создает кольцо огня!",
          webRing = "<boss><boss_name></boss> создает кольцо паутины!",
          teleportStrike = "<boss><boss_name></boss> использует <item>Приемы ниндзя</item>!",
          meteorShower = "<boss><boss_name></boss> вызывает <item>метеоритный дождь</item>!",
          arrowArc = "<boss><boss_name></boss> выпускает <item>дуговой залп</item>!"
        )
      )
    )
  }
}