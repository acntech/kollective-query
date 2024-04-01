package no.acntech.kollectiveq.test

import no.acntech.kollectiveq.Filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class FilterTest {

   @Test
   fun testSimpleCondition() {
      val input = "field1 \$eq: value1"
      val condition = Filter.of(input, true).rootCondition
      val expected = Filter.Condition.SimpleCondition("field1", Filter.Operator.EQ, Filter.Value.StringValue("value1"))
      assertEquals(expected, condition)
   }

   @Test
   fun testSimpleConditionWithSpace() {
      val input = "field1 \$eq: value$ 1"
      val condition = Filter.of(input, true).rootCondition
      val expected = Filter.Condition.SimpleCondition("field1", Filter.Operator.EQ, Filter.Value.StringValue("value 1"))
      assertEquals(expected, condition)
   }

   @Test
   fun testSimpleConditionWithLikeOpAndEscapedWildCard() {
      val input = "field1\$like:value$*"
      val condition = Filter.of(input, true).rootCondition
      val expected = Filter.Condition.SimpleCondition("field1", Filter.Operator.LIKE, Filter.Value.StringValue("value**"))
      assertEquals(expected, condition)
   }

   @Test
   fun testLogicalExpression() {
      val input = "field1\$eq:value1\$and:field2\$gt:10"
      val condition = Filter.of(input, true).rootCondition
      val expected = Filter.Condition.ConditionGroup(
         Filter.Operator.AND,
         mutableListOf(
            Filter.Condition.SimpleCondition("field1", Filter.Operator.EQ, Filter.Value.StringValue("value1")),
            Filter.Condition.SimpleCondition("field2", Filter.Operator.GT, Filter.Value.LongValue(10L))
         )
      )
      assertEquals(expected, condition)
   }

   @Test
   fun testInListPredicate() {
      val input = "field1\$in:[value1,value2,value3]"
      val condition = Filter.of(input, false).rootCondition
      val expected = Filter.Condition.InListCondition(
         "field1",
         Filter.Operator.IN,
         listOf(
            Filter.Value.StringValue("value1"),
            Filter.Value.StringValue("value2"),
            Filter.Value.StringValue("value3")
         )
      )
      assertEquals(expected, condition)
   }

   @Test
   fun testNestedLogicalExpressions() {
      val input = "(field1\$eq:value1\$and:field2\$gt:10)\$or:(field3\$lt:5)"
      val condition = Filter.of(input, true).rootCondition
      val expected = Filter.Condition.ConditionGroup(
         Filter.Operator.OR,
         mutableListOf(
            Filter.Condition.ConditionGroup(
               Filter.Operator.AND,
               mutableListOf(
                  Filter.Condition.SimpleCondition("field1", Filter.Operator.EQ, Filter.Value.StringValue("value1")),
                  Filter.Condition.SimpleCondition("field2", Filter.Operator.GT, Filter.Value.LongValue(10L))
               )
            ),
            Filter.Condition.SimpleCondition("field3", Filter.Operator.LT, Filter.Value.LongValue(5L))
         )
      )
      assertEquals(expected, condition)
   }

   @Test
   fun testNestedLogicalExpressionsWithDateTime() {
      val input = "(field1\$gte:2023-12-01\$and:field1\$lte:2023-12-10)" +
         "\$or:(field2\$gt:2023-11-02T15:22:45.123Z\$and:field2\$lt:2023-11-20T15:22:45.123Z)"
      val filter = Filter.of(input, false)
      val condition = filter.rootCondition
      val expected = Filter.Condition.ConditionGroup(
         Filter.Operator.OR,
         mutableListOf(
            Filter.Condition.ConditionGroup(
               Filter.Operator.AND,
               mutableListOf(
                  Filter.Condition.SimpleCondition(
                     "field1",
                     Filter.Operator.GTE,
                     Filter.Value.DateValue(LocalDate.of(2023, 12, 1))
                  ),
                  Filter.Condition.SimpleCondition(
                     "field1",
                     Filter.Operator.LTE,
                     Filter.Value.DateValue(LocalDate.of(2023, 12, 10))
                  )
               )
            ),
            Filter.Condition.ConditionGroup(
               Filter.Operator.AND,
               mutableListOf(
                  Filter.Condition.SimpleCondition(
                     "field2",
                     Filter.Operator.GT,
                     Filter.Value.UtcDateTimeValue(Instant.parse("2023-11-02T15:22:45.123Z"))
                  ),
                  Filter.Condition.SimpleCondition(
                     "field2",
                     Filter.Operator.LT,
                     Filter.Value.UtcDateTimeValue(Instant.parse("2023-11-20T15:22:45.123Z"))
                  )
               )
            )
         )
      )
      assertEquals(expected, condition)

      println(filter.prettyPrint())

   }

   @Test
   fun testNotExpressionWithoutParentheses() {
      val input = "\$not:field1\$eq:value1"
      val filter = Filter.of(input, true)
      // Assuming Filter.of returns a Filter object that represents the parsed condition
      // Here you would assert that the rootCondition is a NotExpression with the correct sub-conditions
      assertTrue(filter.rootCondition is Filter.Condition.NotCondition)
      assertTrue((filter.rootCondition as Filter.Condition.NotCondition).condition is Filter.Condition.SimpleCondition)
   }

   @Test
   fun testNotExpressionWithParentheses() {
      val input = "\$not:(field1\$eq:value1)"
      val filter = Filter.of(input)
      // Check that the condition is parsed correctly with the NOT having the right scope
      assertTrue(filter.rootCondition is Filter.Condition.NotCondition)
      assertTrue((filter.rootCondition as Filter.Condition.NotCondition).condition is Filter.Condition.SimpleCondition)
      assertTrue((filter.rootCondition as Filter.Condition.NotCondition).condition is Filter.Condition.SimpleCondition)
   }

   @Test
   fun testNotExpressionWithLogicalAnd() {
      val input = "field1\$eq:value1\$and:\$not:field2\$gt:value2"
      val filter = Filter.of(input)
      // Check the parsed structure has an AND with a NOT on the right-hand side
      assertTrue(filter.rootCondition is Filter.Condition.ConditionGroup)
      val conditionGroup = filter.rootCondition as Filter.Condition.ConditionGroup
      assertTrue(conditionGroup.operator == Filter.Operator.AND)
      assertTrue(conditionGroup.conditions.any { it is Filter.Condition.NotCondition })
   }

   @Test
   fun testNotExpressionWithLogicalOr() {
      val input = "field1\$eq:value1\$or:\$not:(field2\$gt:value2)"
      val filter = Filter.of(input)
      // Check the parsed structure has an OR with a NOT group on the right-hand side
      assertTrue(filter.rootCondition is Filter.Condition.ConditionGroup)
      val conditionGroup = filter.rootCondition as Filter.Condition.ConditionGroup
      assertTrue(conditionGroup.operator == Filter.Operator.OR)
      assertTrue(conditionGroup.conditions.any { it is Filter.Condition.NotCondition })
   }

   @Test
   fun testNestedNotExpression() {
      val input = "\$not:(field1\$eq:value1\$and:\$not:field2\$gt:value2)"
      val filter = Filter.of(input)
      // Check that the NOT applies to a group that contains an AND with another NOT
      assertTrue(filter.rootCondition is Filter.Condition.NotCondition)
      val notExpression = filter.rootCondition as Filter.Condition.NotCondition
      assertTrue(notExpression.condition is Filter.Condition.ConditionGroup)
      val innerGroup = notExpression.condition as Filter.Condition.ConditionGroup
      assertTrue(innerGroup.conditions.any { it is Filter.Condition.NotCondition })
   }

}
