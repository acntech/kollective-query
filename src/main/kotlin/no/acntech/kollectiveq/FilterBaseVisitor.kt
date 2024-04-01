package no.acntech.kollectiveq

/**
 * This visitor visits all conditions in the filter and overriding classes can be used to implement custom logic for each
 * condition.
 */
abstract class FilterBaseVisitor : FilterVisitor {

   override fun visit(condition: Filter.Condition) {
      when (condition) {
         is Filter.Condition.HavingCondition -> visitHavingCondition(condition)
         is Filter.Condition.HavingFunctionCondition -> visitHavingFunctionCondition(condition)
         is Filter.Condition.SimpleCondition -> visitSimpleCondition(condition)
         is Filter.Condition.InListCondition -> visitInListCondition(condition)
         is Filter.Condition.NotInListCondition -> visitNotInListCondition(condition)
         is Filter.Condition.ConditionGroup -> visitConditionGroup(condition)
         is Filter.Condition.NotCondition -> visitNotCondition(condition)
      }
   }

   open fun visitHavingFunctionCondition(condition: Filter.Condition.HavingFunctionCondition) {
      // Default implementation does nothing
   }

   open fun visitHavingCondition(condition: Filter.Condition.HavingCondition) {
      // Default implementation does nothing
   }

   open fun visitSimpleCondition(condition: Filter.Condition.SimpleCondition) {
      // Default implementation does nothing
   }

   open fun visitInListCondition(condition: Filter.Condition.InListCondition) {
      // Default implementation does nothing
   }

   open fun visitNotInListCondition(condition: Filter.Condition.NotInListCondition) {
      // Default implementation does nothing
   }

   open fun visitNotCondition(condition: Filter.Condition.NotCondition) {
      // Default implementation does nothing
   }

   /**
    * Default implementation visits all conditions in the group.
    */
   open fun visitConditionGroup(conditionGroup: Filter.Condition.ConditionGroup) {
      conditionGroup.conditions.forEach { visit(it) }
   }


}