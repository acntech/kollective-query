package no.acntech.kollectiveq

import no.acntech.kollectiveq.util.text.*
import no.acntech.kollectiveq.util.time.DetailedParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets

/**
 * Sorting class for building sort criteria from a string.
 */
data class Sorting(
   val criteria: MutableList<SortCriterion> = mutableListOf(),
) {

   private val log: Logger = LoggerFactory.getLogger(javaClass)


   data class SortCriterion(val field: String, val direction: Direction)

   enum class Direction(val representation: String) {
      ASC(EMPTY_STRING),
      DESC(HYPHEN),
      ASC_ALPH(TILDE),
      DESC_ALPH("$TILDE$HYPHEN");

      companion object {
         fun of(dirString: String): Direction {
            return entries.firstOrNull { it.representation == dirString }
               ?: throw DetailedParseException("Unknown sort direction $dirString")
         }
      }
   }

   companion object {

      const val QUERY_PARAM = "sort"

      private val DEFAULT_DIRECTION = Direction.ASC

      private const val SEPARATOR = COMMA

      // Factory method to create a Sorter instance and apply criteria from a string
      fun of(value: String): Sorting {
         val sorting = Sorting()

         // Parse the input string and create criteria
         value.split(SEPARATOR).forEach { sortComponent ->
            var dirString = EMPTY_STRING
            var fieldName = sortComponent

            // Determine direction based on the prefix
            when {
               sortComponent.startsWith(Direction.DESC.representation) -> {
                  dirString = Direction.DESC.representation
                  fieldName = sortComponent.substring(1)
               }

               sortComponent.startsWith(Direction.ASC_ALPH.representation) -> {
                  dirString = Direction.ASC_ALPH.representation

                  fieldName = sortComponent.substring(1)

                  if (sortComponent.startsWith(Direction.DESC_ALPH.representation)) {
                     dirString = Direction.DESC_ALPH.representation
                     fieldName = sortComponent.substring(2)
                  }
               }
            }

            // Determine direction
            val direction = Direction.of(dirString)

            // Add criterion to the list
            sorting.criteria.add(SortCriterion(fieldName, direction))
         }

         return sorting
      }
   }

   fun accept(visitor: SortingVisitor) {
      visitor.visitCriteria(criteria)
   }

   fun asString(): String {
      return criteria.joinToString(separator = COMMA) { "${it.direction.representation}${it.field}" }
   }

   fun toHttpQueryParameter(): String {
      return "$QUERY_PARAM$EQUALS${UriUtils.encodeQueryParam(asString(), StandardCharsets.UTF_8)}"
   }

}