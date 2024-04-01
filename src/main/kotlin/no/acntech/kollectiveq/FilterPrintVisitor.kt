package no.acntech.kollectiveq

import no.acntech.kollectiveq.FilterPrintVisitor.PrintFormat
import no.acntech.kollectiveq.util.text.*

/**
 * The FilterPrintVisitor class is responsible for visiting filter conditions and generating a formatted string representation
 * of the conditions.
 *
 * @property format The print format to use. The default value is [PrintFormat.PRETTY].
 * @property indentSize The number of spaces to use for each level of indentation. The default value is 4.
 */
class FilterPrintVisitor(
   private val format: PrintFormat = PrintFormat.PRETTY,
   private val indentSize: Int = 4,
) : FilterBaseVisitor() {

   enum class PrintFormat {
      PRETTY,
      COMPACT
   }

   private val sb = StringBuilder()

   private var currentIndent = 0

   private val indent: String
      get() = SPACE.repeat(currentIndent * indentSize)

   override fun visitSimpleCondition(condition: Filter.Condition.SimpleCondition) {
      appendWithFormat(
         "${condition.field} ${condition.operator.representation}${
            condition.value?.let {
               " ${it.value}"
            } ?: EMPTY_STRING
         }")

   }

   override fun visitInListCondition(condition: Filter.Condition.InListCondition) {
      appendWithFormat("${condition.field} ${condition.operator.representation} [${condition.values.joinToString()}]")
   }

   override fun visitNotInListCondition(condition: Filter.Condition.NotInListCondition) {
      appendWithFormat("${condition.field} ${condition.operator.representation} [${condition.values.joinToString()}]")
   }

   override fun visitConditionGroup(conditionGroup: Filter.Condition.ConditionGroup) {
      appendWithFormat(LEFT_PAREN)
      currentIndent++
      conditionGroup.conditions.forEachIndexed { index, condition ->
         if (index > 0) {
            appendWithNewLineAndIndent()
            appendWithFormat("${conditionGroup.operator.representation} ")
         }
         visit(condition)
      }
      currentIndent--
      appendWithFormat(RIGHT_PAREN)
   }

   fun print(): String {
      return sb.toString()
   }

   override fun toString(): String = print()

   private fun appendWithFormat(str: String) {
      when (format) {
         PrintFormat.PRETTY -> sb.append("$str ")
         PrintFormat.COMPACT -> sb.append(str.trim())
      }
   }

   private fun appendWithNewLineAndIndent() {
      if (format == PrintFormat.PRETTY) {
         sb.append("$NEW_LINE$indent")
      }
   }
}