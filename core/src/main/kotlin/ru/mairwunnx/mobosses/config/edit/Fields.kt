package ru.mairwunnx.mobosses.config.edit

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTaggedNode
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import ru.mairwunnx.mobosses.config.Yamls

/**
 * The values of a model, walked from its serializer, so a new key in a model shows on the screen without new screen
 * code. Booleans, numbers, and strings are values; classes go deeper; maps go deeper by the keys the file and the
 * defaults have, like the classes of `rewards.yml`. Lists and enums stay in the file, for its text to change.
 */
object Fields {
  fun of(descriptor: SerialDescriptor, text: String, defaults: String): List<Field> {
    val current = runCatching { Yamls.parse(text.ifBlank { "{}" }) }.getOrNull()
    val fallback = runCatching { Yamls.parse(defaults) }.getOrNull()
    val notes = Notes.of(text)
    return buildList { walk(descriptor, emptyList(), current, fallback, notes, this) }
  }

  private fun walk(descriptor: SerialDescriptor, path: List<String>, current: YamlNode?, fallback: YamlNode?, notes: Map<List<String>, String>, into: MutableList<Field>) {
    when (descriptor.kind) {
      StructureKind.CLASS -> for (i in 0 until descriptor.elementsCount) {
        val name = descriptor.getElementName(i)
        walk(descriptor.getElementDescriptor(i), path + name, child(current, name), child(fallback, name), notes, into)
      }
      StructureKind.MAP -> {
        val keys = (keys(fallback) + keys(current)).distinct()
        val values = descriptor.getElementDescriptor(1)
        keys.forEach { key -> walk(values, path + key, child(current, key), child(fallback, key), notes, into) }
      }
      is PrimitiveKind -> {
        val kind = kind(descriptor.kind as PrimitiveKind) ?: return
        // A key with no default that the file leaves out, like 1.x's `language`, is there only for old files.
        if (descriptor.isNullable && scalar(fallback) == null && scalar(current) == null) return
        val default = scalar(fallback).orEmpty()
        into += Field(path, kind, scalar(current) ?: default, default, notes[path].orEmpty())
      }
      else -> {}
    }
  }

  private fun kind(kind: PrimitiveKind): Field.Kind? = when (kind) {
    PrimitiveKind.BOOLEAN -> Field.Kind.BOOLEAN
    PrimitiveKind.INT, PrimitiveKind.SHORT, PrimitiveKind.BYTE -> Field.Kind.INT
    PrimitiveKind.LONG -> Field.Kind.LONG
    PrimitiveKind.DOUBLE, PrimitiveKind.FLOAT -> Field.Kind.DOUBLE
    PrimitiveKind.STRING -> Field.Kind.STRING
    else -> null
  }

  private fun unwrap(node: YamlNode?): YamlNode? = if (node is YamlTaggedNode) unwrap(node.innerNode) else node

  private fun child(node: YamlNode?, key: String): YamlNode? = (unwrap(node) as? YamlMap)?.entries?.entries?.firstOrNull { it.key.content == key }?.value

  private fun keys(node: YamlNode?): List<String> = (unwrap(node) as? YamlMap)?.entries?.keys?.map { it.content }.orEmpty()

  private fun scalar(node: YamlNode?): String? = (unwrap(node) as? YamlScalar)?.content
}
