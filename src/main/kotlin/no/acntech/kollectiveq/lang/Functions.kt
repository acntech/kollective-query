package no.acntech.kollectiveq.lang

import com.google.common.base.CaseFormat
import no.acntech.kollectiveq.collections.prettyPrint
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

val snakeToCamelTransformer: TransformFunction<String, String> =
   createCaseFormatTransformFunction(CaseFormat.LOWER_UNDERSCORE, CaseFormat.LOWER_CAMEL)

/**
 * Transform function that takes an input of type [T] and returns an output of type [R].
 */
typealias TransformFunction<T, R> = (T) -> R

/**
 * Transform function that takes an input of type [T] and returns the the input itself.
 */
fun <T> identityTransform(): TransformFunction<T, T> = { it } // it refers to the input itself

/**
 * Transform function that will transform a String from one [CaseFormat] to another.
 * @param fromFormat The [CaseFormat] to transform from.
 * @param toFormat The [CaseFormat] to transform to.
 */
fun createCaseFormatTransformFunction(fromFormat: CaseFormat, toFormat: CaseFormat): TransformFunction<String, String> {
   return { input: String ->
      fromFormat.to(toFormat, input)
   }
}

/**
 * Converts an object to a map of property names and values, overriding any values with the same key with the fallback map.
 */
fun Any.toMap(defaultOverrides: Map<String, Any> = emptyMap()): Map<String, Any?> {
   val props = mutableMapOf<String, Any?>()
   this::class.memberProperties
      .filter { !it.getter.isAccessible } // Only include properties with accessible getters
      .forEach { prop ->
         try {
            // Try to call the getter of the property
            props[prop.name] = prop.call(this)
         } catch (e: Exception) {
            props[prop.name] = "Error calling getter: ${e.message}"
         }
      }

   // This will add all key-value pairs from defaultOverrides to props, overriding any existing keys
   props.putAll(defaultOverrides)

   // Since props is already a MutableMap, we can return it directly

   return props
}

/**
 * Pretty prints an object to a string.
 */
fun Any.prettyPrintMe(fallbackMap: Map<String, Any> = emptyMap()): String {
   val defaultToString = "${javaClass.name}@${Integer.toHexString(System.identityHashCode(this))}\n"
   val map: Map<String, Any?> = this.toMap(fallbackMap)
   return defaultToString + map.prettyPrint()
}