package no.acntech.kollectiveq.jpql


import no.acntech.kollectiveq.Sorting
import no.acntech.kollectiveq.SortingVisitor
import no.acntech.kollectiveq.lang.TransformFunction
import no.acntech.kollectiveq.lang.identityTransform
import no.acntech.kollectiveq.text.COMMA_SPACE
import no.acntech.kollectiveq.text.DetailedParseException
import no.acntech.kollectiveq.text.EMPTY_STRING
import no.acntech.kollectiveq.text.SPACE

/**
 * Visitor that transforms a [Sorting] to a JPQL ORDER BY clause.
 */
class JPQLTransformationSortingVisitor(
   private val fieldTransformer: TransformFunction<String, String> = identityTransform(),
   private val legalFields: Set<String>? = null,
) : SortingVisitor {

   companion object {
      private const val ASCENDING = "ASC"
      private const val DESCENDING = "DESC"
   }

   private val sb = StringBuffer()

   override fun visitCriterion(criterion: Sorting.SortCriterion) {
      val transformedFieldName = fieldTransformer(criterion.field)
      checkLegalField(transformedFieldName)

      // Determine the JPQL sorting direction and apply the 'str' function if needed
      val jpqlDirection = when (criterion.direction) {
         Sorting.Direction.ASC, Sorting.Direction.ASC_ALPH -> ASCENDING
         else -> DESCENDING
      }

      // Apply 'str' function for alphabetic sorting if required
      val jpqlField = when (criterion.direction) {
         Sorting.Direction.ASC_ALPH, Sorting.Direction.DESC_ALPH -> "str($transformedFieldName)"
         else -> transformedFieldName
      }

      sb.append(jpqlField).append(SPACE).append(jpqlDirection).append(COMMA_SPACE)
   }


   fun toSortingClause(): String {
      return if (sb.isNotEmpty()) sb.toString().dropLast(2) else EMPTY_STRING
   }

   override fun toString(): String {
      return "JPQLSortingVisitor(sb=$sb)"
   }

   private fun checkLegalField(field: String) {
      if (legalFields != null && !legalFields.contains(field)) {
         throw DetailedParseException("Illegal field: $field")
      }
   }
}