package ru.mairwunnx.mobosses.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlException
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlPathSegment
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTaggedNode
import kotlinx.serialization.DeserializationStrategy

/**
 * Reads the config files. Unknown keys are ignored, and a value that doesn't fit its field (a word
 * where a number goes, say) drops out with a warning, so that field takes its default and the rest
 * of the file still counts. Only broken YAML fails a whole file.
 */
object Yamls {
  val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

  fun <T> decode(deserializer: DeserializationStrategy<T>, text: String, warn: (String) -> Unit = {}): T {
    var node = if (text.isBlank()) parse("{}") else parse(text)
    // A file with one bad value per field still ends, since each round removes a node.
    repeat(500) {
      try {
        return yaml.decodeFromYamlNode(deserializer, node)
      } catch (e: YamlException) {
        val steps = e.path.segments.mapNotNull {
          when (it) {
            is YamlPathSegment.MapElementKey -> Step.Key(it.key)
            is YamlPathSegment.ListEntry -> Step.Index(it.index)
            else -> null
          }
        }
        node = without(node, steps) ?: throw e
        warn("${steps.joinToString(".") { step -> step.toString() }.ifEmpty { "(file)" }}: ${e.message?.substringBefore('\n')}; using the default")
      }
    }
    return yaml.decodeFromYamlNode(deserializer, node)
  }

  fun parse(text: String): YamlNode = yaml.parseToYamlNode(text)

  /** The language files and other free-form maps: nested keys join with dots, and lists join with new lines. */
  fun flatten(node: YamlNode, prefix: String = "", into: MutableMap<String, String> = linkedMapOf()): Map<String, String> {
    when (node) {
      is YamlMap -> node.entries.forEach { (key, value) -> flatten(value, if (prefix.isEmpty()) key.content else "$prefix.${key.content}", into) }
      is YamlList -> into[prefix] = node.items.joinToString("\n") { (it as? YamlScalar)?.content ?: "" }
      is YamlScalar -> into[prefix] = node.content
      is YamlNull -> into[prefix] = ""
      is YamlTaggedNode -> flatten(node.innerNode, prefix, into)
      else -> {}
    }
    return into
  }

  private sealed interface Step {
    data class Key(val key: String) : Step {
      override fun toString() = key
    }

    data class Index(val index: Int) : Step {
      override fun toString() = "[$index]"
    }
  }

  /** The tree without the node at [steps]; null when the steps lead nowhere, so there's nothing to drop. */
  private fun without(node: YamlNode, steps: List<Step>): YamlNode? {
    val step = steps.firstOrNull() ?: return null
    val rest = steps.drop(1)
    return when {
      node is YamlMap && step is Step.Key -> {
        val key = node.entries.keys.firstOrNull { it.content == step.key } ?: return null
        val child = if (rest.isEmpty()) null else without(node.entries.getValue(key), rest)
        YamlMap(if (child == null) node.entries - key else node.entries + (key to child), node.path)
      }
      node is YamlList && step is Step.Index -> {
        val item = node.items.getOrNull(step.index) ?: return null
        val child = if (rest.isEmpty()) null else without(item, rest)
        YamlList(if (child == null) node.items.filterIndexed { i, _ -> i != step.index } else node.items.mapIndexed { i, it -> if (i == step.index) child else it }, node.path)
      }
      node is YamlTaggedNode -> without(node.innerNode, steps)
      else -> null
    }
  }
}
