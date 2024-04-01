package no.acntech.kollectiveq

import no.acntech.kollectiveq.util.lang.ValidationException

/**
 * This visitor validates that the filter does not contain any conditions that are not allowed.
 */
class FilterValidationVisitor : FilterBaseVisitor() {

   override fun visitSimpleCondition(condition: Filter.Condition.SimpleCondition) {
      throw ValidationException("Simple condition not allowed")
   }

   override fun visitInListCondition(condition: Filter.Condition.InListCondition) {
      throw ValidationException("InList condition not allowed")
   }

   override fun visitNotInListCondition(condition: Filter.Condition.NotInListCondition) {
      throw ValidationException("NotInList condition not allowed")
   }

   override fun visitNotCondition(condition: Filter.Condition.NotCondition) {
      throw ValidationException("Not condition not allowed")
   }
}