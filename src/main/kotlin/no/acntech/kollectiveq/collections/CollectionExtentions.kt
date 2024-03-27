package no.acntech.kollectiveq.collections

import no.acntech.kollectiveq.text.EMPTY_STRING
import no.acntech.kollectiveq.text.NEW_LINE
import no.acntech.kollectiveq.text.SPACE

fun List<*>.prettyPrint(indentation: Int = 0): String {
   val indent = SPACE.repeat(indentation)
   return joinToString(separator = "\n$indent") { "$indent${it.toString()}" }
}

fun Map<*, *>.prettyPrint(
   sortKeys: Boolean = false,
   offset: Int = 4,
   paddingChar: Char = ' ',
   keyValueSeparator: String = ": ",
): String {
   val effectiveMap = if (sortKeys) entries.sortedBy { it.key?.toString() ?: EMPTY_STRING } else entries

   if (effectiveMap.isEmpty()) {
      return EMPTY_STRING
   }

   val longestKeyLength = effectiveMap.maxOf { it.key?.toString()?.length ?: 0 }
   val padding = EMPTY_STRING.padStart(offset, paddingChar)

   return effectiveMap.joinToString(separator = NEW_LINE) { entry ->
      val keyPadding = EMPTY_STRING.padStart(longestKeyLength - (entry.key?.toString()?.length ?: 0), ' ')
      val valueString = when (val value = entry.value) {
         is Map<*, *> -> "\n${value.prettyPrint(sortKeys, offset * 2, paddingChar, keyValueSeparator)}"
         else -> value?.toString() ?: "null"
      }
      "${padding}${entry.key?.toString()}$keyPadding$keyValueSeparator$valueString"
   }
}

fun <K, V> mapFromPairs(vararg pairs: Any?): Map<K, V> {
   if (pairs.size % 2 != 0) {
      throw IllegalArgumentException("Odd number of arguments")
   }

   val map = mutableMapOf<K, V>()
   for (i in pairs.indices step 2) {
      @Suppress("UNCHECKED_CAST")
      val key = pairs[i] as? K ?: throw IllegalArgumentException("Key at index $i is null or not of the correct type")

      @Suppress("UNCHECKED_CAST")
      val value =
         pairs[i + 1] as? V ?: throw IllegalArgumentException("Value at index ${i + 1} is null or not of the correct type")

      map[key] = value
   }
   return map.toMap()
}


