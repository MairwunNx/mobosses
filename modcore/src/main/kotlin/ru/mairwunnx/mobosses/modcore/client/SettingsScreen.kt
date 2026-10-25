package ru.mairwunnx.mobosses.modcore.client

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import ru.mairwunnx.mobosses.config.edit.Field
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.net.Payloads
import java.util.Locale

/**
 * Mo’Bosses' settings in a player's game, as the game's own game rules screen looks: a file to pick, a search box,
 * every value of the file under its section, with the file's own comment and the default on hover, and Done to save.
 * Booleans flip, numbers and words type in, red while they aren't a value of their kind. The values come from a
 * [SettingsSource]: the server's files in a world with Mo’Bosses as a mod, the game's own in the main menu.
 */
class SettingsScreen(private val parent: Screen?) : Screen(ClientTexts.text(Message.SETTINGS_TITLE)) {
  private var layout = layout()
  private val source = SettingsSource.pick()
  private var state: SettingsSource.State? = null
  private var file = 0
  private var filter = ""
  /** What the player typed, by file and path, until Done sends it. */
  private val pending = LinkedHashMap<String, LinkedHashMap<List<String>, String>>()
  private val invalid = HashSet<List<String>>()
  private var list: FieldList? = null
  private var status: StringWidget? = null
  private var done: Button? = null

  /** A fresh one each time the screen builds itself, as it does when the values arrive. */
  private fun layout() = HeaderAndFooterLayout(this, 12 + 9 + 4 + 15 + 4 + 20, 33 + 12)

  override fun init() {
    layout = layout()
    val header = layout.addToHeader(LinearLayout.vertical().spacing(4))
    header.defaultCellSetting().alignHorizontallyCenter()
    header.addChild(StringWidget(title, font))
    val search = header.addChild(EditBox(font, 200, 15, Component.empty()))
    search.setHint(ClientTexts.text(Message.SETTINGS_SEARCH).copy().withStyle(EditBox.SEARCH_HINT_STYLE))
    search.value = filter
    search.setResponder { filter = it.lowercase(Locale.ROOT); list?.fill() }
    val files = state?.files.orEmpty()
    if (files.isNotEmpty()) {
      header.addChild(CycleButton.builder<Int>({ Component.literal(files[it].name) }, file).withValues(files.indices.toList()).displayOnlyValue()
        .create(0, 0, 200, 20, Component.empty()) { _, index -> file = index; list?.fill() })
    }
    list = layout.addToContents(FieldList())
    val footer = layout.addToFooter(LinearLayout.vertical().spacing(4))
    footer.defaultCellSetting().alignHorizontallyCenter()
    status = footer.addChild(StringWidget(300, 9, statusText(), font))
    val buttons = footer.addChild(LinearLayout.horizontal().spacing(8))
    done = buttons.addChild(Button.builder(CommonComponents.GUI_DONE) { save() }.build())
    buttons.addChild(Button.builder(CommonComponents.GUI_CANCEL) { onClose() }.build())
    layout.visitWidgets { addRenderableWidget(it) }
    repositionElements()
    if (state == null) source?.load(::shown)
  }

  private fun statusText(): Component {
    val state = state
    return when {
      source == null -> ClientTexts.text(Message.SETTINGS_ELSEWHERE)
      state == null -> ClientTexts.text(Message.SETTINGS_WAITING)
      state.message.isNotEmpty() -> Component.literal(state.message).withStyle(ChatFormatting.GRAY)
      state.note != null -> ClientTexts.text(state.note)
      else -> Component.empty()
    }
  }

  private fun shown(next: SettingsSource.State) {
    state = next
    file = file.coerceIn(0, (next.files.size - 1).coerceAtLeast(0))
    rebuildWidgets()
  }

  /** Sends what the player typed, a file at a time; closes once every file took it all. */
  private fun save() {
    val source = source ?: return onClose()
    val next = pending.entries.firstOrNull { it.value.isNotEmpty() } ?: return onClose()
    val changes = next.value.map { (path, value) -> Payloads.Change(path, value) }
    pending.remove(next.key)
    done?.active = false
    source.save(next.key, changes) { answer ->
      shown(answer)
      // A refusal stays on the screen, with its reason under the list; the rest goes on.
      if (!answer.failed) save()
    }
  }

  override fun repositionElements() {
    layout.arrangeElements()
    list?.updateSize(width, layout)
  }

  override fun onClose() {
    minecraft.gui.setScreen(parent)
  }

  private fun fields(): List<Field> = state?.files?.getOrNull(file)?.fields.orEmpty()

  private fun typed(field: Field): String? = state?.files?.getOrNull(file)?.name?.let { pending[it]?.get(field.path) }

  private fun type(field: Field, value: String) {
    val name = state?.files?.getOrNull(file)?.name ?: return
    if (Field.accepts(field.kind, value.trim())) invalid -= field.path else invalid += field.path
    pending.getOrPut(name, ::LinkedHashMap).apply { if (value == field.value) remove(field.path) else put(field.path, value) }
    done?.active = invalid.isEmpty()
  }

  private fun tooltip(field: Field): List<FormattedCharSequence> = buildList {
    add(Component.literal(field.key).withStyle(ChatFormatting.YELLOW).visualOrderText)
    if (field.note.isNotEmpty()) addAll(font.split(Component.literal(field.note), 200))
    add(ClientTexts.text(Message.SETTINGS_DEFAULT, Placeholder.unparsed("default", field.default)).visualOrderText)
  }

  abstract class Row(val tooltip: List<FormattedCharSequence>?) : ContainerObjectSelectionList.Entry<Row>()

  private inner class Section(private val label: Component) : Row(null) {
    private val widget = StringWidget(label.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), font)

    override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
      widget.setPosition(contentXMiddle - widget.width / 2, contentYMiddle - widget.height / 2)
      widget.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun children(): List<GuiEventListener> = emptyList()
    override fun narratables(): List<NarratableEntry> = listOf(widget)
  }

  private inner class Value(private val field: Field) : Row(tooltip(field)) {
    private val label = font.split(Component.literal(field.path.last()), 170)
    private val widget: AbstractWidget = if (field.kind == Field.Kind.BOOLEAN) {
      CycleButton.onOffBuilder((typed(field) ?: field.value) == "true").displayOnlyValue()
        .create(0, 0, 44, 20, Component.literal(field.key)) { _, on -> type(field, on.toString()) }
    } else {
      EditBox(font, 0, 0, 90, 20, Component.literal(field.key)).also { box ->
        box.setMaxLength(256)
        box.value = typed(field) ?: field.value
        box.setResponder { value ->
          box.setTextColor(if (Field.accepts(field.kind, value.trim())) -2039584 else -65536)
          type(field, value)
        }
      }
    }

    override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
      label.firstOrNull()?.let { graphics.text(font, it, contentX, contentY + 5, -1) }
      widget.x = contentRight - widget.width - 1
      widget.y = contentY
      widget.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun children(): List<GuiEventListener> = listOf(widget)
    override fun narratables(): List<NarratableEntry> = listOf(widget)
  }

  private inner class FieldList : ContainerObjectSelectionList<Row>(Minecraft.getInstance(), this@SettingsScreen.width, layout.contentHeight, layout.headerHeight, 24) {
    init {
      fill()
    }

    fun fill() {
      clearEntries()
      val shown = fields().filter { filter.isEmpty() || filter in it.key.lowercase(Locale.ROOT) || filter in it.note.lowercase(Locale.ROOT) }
      shown.groupBy { it.path.dropLast(1).joinToString(".") }.forEach { (section, fields) ->
        if (section.isNotEmpty()) addEntry(Section(Component.literal(section)))
        fields.forEach { addEntry(Value(it)) }
      }
      setScrollAmount(0.0)
    }

    override fun getRowWidth() = 310

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
      super.extractWidgetRenderState(graphics, mouseX, mouseY, a)
      hovered?.tooltip?.let { graphics.setTooltipForNextFrame(it, mouseX, mouseY) }
    }
  }
}
