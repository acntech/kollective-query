package no.acntech.kollectiveq

import no.acntech.kollectiveq.antlr4.FilterGrammarBaseVisitor
import no.acntech.kollectiveq.antlr4.FilterGrammarLexer
import no.acntech.kollectiveq.antlr4.FilterGrammarParser
import no.acntech.kollectiveq.util.text.EQ
import no.acntech.kollectiveq.util.time.DetailedParseException
import no.acntech.kollectiveq.util.time.FlexibleInstantParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets
import java.time.*
import java.time.format.DateTimeParseException

/**
 * This class provides filtering functionality on collections with a query string parameter `filter`.
 * The filter syntax is inspired by MongoDB, made up of predicates containing a property name,
 * an operator, and a value.
 *
 * Syntax Example: `filter=name$eq:Joe` - This matches all persons with the value "Joe" in the 'name' property.
 *
 * Predicates can be chained with logical operators AND, OR, and NOT (to negate a predicate).
 *
 * Chaining Example: `filter=name$eq:Joe$and:city$like:*port$and:country$ne:Norway`
 * This matches all persons named Joe, where the city includes 'port' and the country is not Norway.
 *
 * String filtering is case-insensitive.
 *
 * Operator affinity can be controlled using parentheses.
 *
 * Affinity Example: `filter=name$eq:Joe$and:$not:(city$like:*port$or:age$lt:40)`
 * This matches all persons named Joe where the city does not include 'port' or age is less than 40.
 *
 * All filter strings should be URL encoded.
 *
 * The class supports the following filter operators:
 * - Equals (`$eq:`)
 * - Not equals (`$ne:`)
 * - Greater than (`$gt:`)
 * - Greater than or equal (`$gte:`)
 * - Less than (`$lt:`)
 * - Less than or equal (`$lte:`)
 * - Is NULL (`$null:`)
 * - Is NOT NULL (`$nnull:`)
 * - Substring match (`$like:`)
 * - Logical AND (`$and:`)
 * - Logical OR (`$or:`)
 * - Logical NOT (`$not:`)
 * - In a list (`$in:`)
 * - Not in a list (`$nin:`)
 * - Having a relational condition (`$having:(<relationship-field><operator><value>)`)
 * - Having a relational function condition (`$having:FUNCTION(<field.subfield>)<operator><value>`) where FUNCTION is one of
 * COUNT, SUM, AVG, MIN, MAX
 *
 * The `$having:` (without a function) operator is used to filter by a condition on a one-to-many or many-to-many relationship.
 * Example: On the /departments/ endpoint: filter=$having:employees(name$eq:John) - matches all departments with an employee
 * named John.
 *
 * The `$having:FUNCTION` operator is used to filter by a conditional function on a one-to-many or many-to-many relationship.
 * Example 1: On the /departments/ endpoint: filter=$having:COUNT(employees)$gt:10 - matches all departments with more than 10
 * employees.
 * Example 2: On the /departments/ endpoint: filter=$having:AVG(employees.salary)$gt:500000 - matches all departments where the
 * average salary is greater than 100.000
 *
 * Note that the `$having:` operator does <i>not</i> support nesting - it is only intended to be used on the top level of a filter.
 *
 * The `$like:` operator supports wildcards for multi-character (*) and single-character (?) matches. Note that string comparisons
 * are (normally) done lowercase, depending on the underlying database support.
 *
 * Example: `filter=name$like:K*so?` - matches all resources with a name starting with "K", then s sequence of random characters,
 * ending with "so" followed by any single character.
 *
 * If no wildcard is present, the string will be prefixed and suffixed with a (*), effectively turning it into a "contains"
 * expression.
 *
 * Null values can be filtered using `$null:` and `$nnull:`.
 *
 * Time-related values support various UTC-based formats for filtering.
 * Specific time fields can be extracted for comparison.
 *
 * The `$in:` and `$nin:` operators are used to filter by a list of values, which must be enclosed in brackets.
 *
 * Examples of time field filtering:
 * - `filter=birth_date$eq:01-25` matches all resources with a birthdate on January 25th.
 * - `filter=birth_date$gte:1995--` matches all resources born in or after 1995.
 * - `filter=last_login$gte:12:15:00` matches all resources who last logged in after 12:15:00.
 * - `filter=last_login$gte:2023-01-01T00:00:00Z` matches all resources who last logged in after 2023-01-01T00:00:00Z.
 * - `filter=last_login$gte:2023-11-02T15:22:45.123+0200` matches all resources who last logged in after 2023-11-02T15:22:45.123+0200.
 * - `filter=last_login$gte:2023-11-02T15:22[America/New_York]` matches all resources who last logged in after 2023-11-02T15:22 in the America/New_York timezone.
 *
 * Special characters are escaped with a dollar sign ($). Special characters include:
 * - The escape char itself: '$'
 * - Asterisk: '*'
 * - Question mark: '?'
 * - Left parenthesis: '('
 * - Right parenthesis: ')'
 * - Comma: ','
 * - Left bracket: '['
 * - Right bracket: ']'
 * - Colon: ':'
 * - Hyphen: '-'
 * - Space: ' '
 *
 * Note that after escpaping is resolved, literal wildcards (*) and (?) are escaped with (*) and (?), respectively.
 *
 * Example: `filter=some_attribute$like:K$*soL$?p?` becomes `filter=name$like:K**soL??p?`
 *
 * This is done to prepare the string for LIKE or language specific regular expressions in the target query language.
 */
class Filter {

   enum class Operator(val representation: String) {
      EQ("\$eq:"),
      NE("\$ne:"),
      GT("\$gt:"),
      GTE("\$gte:"),
      LT("\$lt:"),
      LTE("\$lte:"),
      LIKE("\$like:"),
      AND("\$and:"),
      OR("\$or:"),
      NOT("\$not:"),
      IN("\$in:"),
      NIN("\$nin:"),
      NULL("\$null:"),
      NNULL("\$nnull:");

      companion object {

         fun of(opString: String): Operator {
            // Assuming operator strings are exactly as the enum names but lowercased
            return values().firstOrNull { it.representation == opString }
               ?: throw DetailedParseException("Unknown operator $opString")
         }
      }
   }


   /**
    * Represents a sealed class `Value` that can hold different types of values.
    *
    * The sealed class `Value` has the following subclasses:
    * - `StringValue` for storing a `String` value.
    * - `LongValue` for storing a `Long` value.
    * - `DoubleValue` for storing a `Double` value.
    * - `BooleanValue` for storing a `Boolean` value.
    * - `DateValue` for storing a `LocalDate` value.
    * - `TimeValue` for storing a `LocalTime` value.
    * - `DateTimeValue` for storing a `LocalDateTime` value.
    * - `MonthDayValue` for storing a `MonthDay` value.
    * - `YearValue` for storing an `Int` value.
    * - `UtcDateTimeValue` for storing an `Instant` value.
    *
    * Each subclass extends the `Value` class and overrides the `value` property with the respective type.
    *
    * @param V the type of the value stored by the class
    * @property value the value stored by the class
    */
   sealed class Value<out V> {
      abstract val value: V

      data class StringValue(override val value: String) : Value<String>()
      data class LongValue(override val value: Long) : Value<Long>()
      data class DoubleValue(override val value: Double) : Value<Double>()

      data class BooleanValue(override val value: Boolean) : Value<Boolean>()
      data class DateValue(override val value: LocalDate) : Value<LocalDate>()
      data class TimeValue(override val value: LocalTime) : Value<LocalTime>()
      data class DateTimeValue(override val value: LocalDateTime) : Value<LocalDateTime>()
      data class MonthDayValue(override val value: MonthDay) : Value<MonthDay>()
      data class YearValue(override val value: Int) : Value<Int>()
      data class UtcDateTimeValue(override val value: Instant) : Value<Instant>()
   }


   /**
    * Represents a condition used in filtering data.
    */
   sealed class Condition {

      /**
       * Represents a simple condition used in filtering.
       *
       * @property field The field on which the condition is applied.
       * @property operator The operator used for comparison.
       * @property value The value to compare with.
       */
      data class SimpleCondition(
         val field: String,
         val operator: Operator,
         val value: Value<*>?,
      ) : Condition() {

         init {
            log.trace("Created: SimpleCondition $this")
         }
      }

      /**
       * Class representing an "in list" condition.
       *
       * @property field The field to be compared.
       * @property operator The comparison operator to be used.
       * @property values The list of values to be checked against the field.
       * @constructor Creates an InListCondition with the given field, operator, and values.
       */
      data class InListCondition(
         val field: String,
         val operator: Operator,
         val values: List<Value<*>>,
      ) : Condition() {

         init {
            log.trace("Created InListCondition: $this")
         }

      }

      /**
       * Represents a condition where a field value is not in a given list of values.
       *
       * @property field the field name
       * @property operator the operator of the condition (NIN - not in)
       * @property values the list of values to check against
       */
      data class NotInListCondition(
         val field: String,
         val operator: Operator,
         val values: List<Value<*>>,
      ) : Condition() {

         init {
            log.trace("Created NotInListCondition: $this")
         }
      }

      /**
       * Represents a negated condition.
       *
       * @property condition The condition to be negated.
       */
      data class NotCondition(
         val condition: Condition,
      ) : Condition() {

         init {
            log.trace("Created NotCondition: $this")
         }
      }

      /**
       * Representation of a condition group that combines multiple conditions with a logical AND or OR.
       *
       * @param operator The logical operator used to combine the conditions (`AND` or `OR`).
       * @param conditions The list of conditions to be combined together.
       */
      data class ConditionGroup(
         val operator: Operator,
         val conditions: MutableList<Condition> = mutableListOf(),
      ) : Condition() {

         init {
            log.trace("Created ConditionGroup: $this")
         }

         fun addCondition(condition: Condition): ConditionGroup {
            conditions.add(condition)
            return this
         }

      }

      /**
       * Represents a filter-by condition for one-to-many relationships.
       *
       * @property field The field on which the having condition is applied.
       * @property subFilter The sub-filter used for the having condition.
       *
       * @constructor Creates a new instance of the [HavingCondition].
       */
      data class HavingCondition(
         val field: String,
         val subFilter: Filter,
      ) : Condition() {
         init {
            log.trace("Created: HavingCondition $this")
         }
      }

      /**
       * Represents a condition that checks if a given field value meets a certain function operation condition.
       *
       * @property field The field name to be checked.
       * @property function The function to be performed on the field value.
       * @property operator The operator to be used for comparison.
       * @property value The value to be compared with the function result.
       * @constructor Creates a new instance of [HavingFunctionCondition].
       */
      data class HavingFunctionCondition(
         val field: String,
         val function: Function,
         val operator: Operator,
         val value: Value<*>,
      ) : Condition() {

         enum class Function {
            COUNT,
            SUM,
            AVG,
            MIN,
            MAX
         }

         init {
            log.trace("Created: HavingFunctionCondition $this")
         }
      }
   }

   companion object {

      private val log: Logger = LoggerFactory.getLogger(Filter::class.java)

      const val QUERY_PARAM = "filter"

      const val ESCAPE_CHAR = '$'

      val ESCAPED_REGULAR_CHARS = setOf(
         '(', // Left parenthesis
         ')', // Right parenthesis
         ',', // Comma
         '[', // Left bracket
         ']', // Right bracket
         ':', // Colon
         '-', // Hyphen
         ' ', // Space
      )

      const val SINGLE_WILDCARD_CHAR = '?'
      const val MULTI_WILDCARD_CHAR = '*'

      val ESCAPED_WILDCARD_CHARS = setOf(
         SINGLE_WILDCARD_CHAR,
         MULTI_WILDCARD_CHAR
      )

      const val ESCAPED_SINGLE_WILDCARD_SEQUENCE = "??"

      const val ESCAPED_MULTI_WILDCARD_SEQUENCE = "**"

      private const val ESC_ESC_SAFEGUARD = "<DOLLAR_DOLLAR>"

      /**
       * Resolves escape sequences in a given string.
       *
       * @param input the string to resolve escape sequences in
       * @return the string with resolved escape sequences
       */
      fun resolveEscapes(input: String): String {
         log.trace("resolveEscapes: $input")

         // Step 1: Safeguard the double escape char
         var resolved = input.replace("$ESCAPE_CHAR$ESCAPE_CHAR", ESC_ESC_SAFEGUARD)

         // Step 2: Replace single wildcard escapes with their JPQL counterparts
         resolved = resolved.replace("$ESCAPE_CHAR$MULTI_WILDCARD_CHAR", ESCAPED_MULTI_WILDCARD_SEQUENCE)
            .replace("$ESCAPE_CHAR$SINGLE_WILDCARD_CHAR", ESCAPED_SINGLE_WILDCARD_SEQUENCE)

         // Step 3: Replace the double escape char safeguard with a single escape char
         resolved = resolved.replace(ESC_ESC_SAFEGUARD, ESCAPE_CHAR.toString())

         // Step 4: Unescape regular characters that were escaped with a single escape char
         ESCAPED_REGULAR_CHARS.forEach { char ->
            resolved = resolved.replace("$ESCAPE_CHAR$char", char.toString())
         }

         return resolved.also {
            log.trace("resolveEscapes result: $it")
         }
      }

      /**
       * Parses the input string using a lexer and parser, constructs a condition tree using a visitor,
       * and returns a Filter object.
       *
       * @param input The input string to parse and construct the filter from.
       * @param trace If true, enables tracing in the parser.
       * @return The constructed Filter object.
       * @throws DetailedParseException if there is an error in parsing or constructing the filter.
       */
      fun of(input: String, trace: Boolean = false): Filter {
         log.trace("Filter input: $input")

         // Create a lexer that feeds off of input CharStream
         val lexer = FilterGrammarLexer(CharStreams.fromString(input))

         // Create a buffer of tokens pulled from the lexer
         val tokens = CommonTokenStream(lexer)

         // Create a parser that feeds off the tokens buffer
         val parser = FilterGrammarParser(tokens)
         parser.isTrace = trace

         // Begin parsing at rule 'filter', which is the entry point of the grammar
         var context: FilterGrammarParser.FilterContext?
         try {
            context = parser.filter()
         } catch (e: RecognitionException) {
            throw DetailedParseException(
               "Failed to parse filter '${e.message}' in line ${e.offendingToken.line}" +
                  " at position ${e.offendingToken.charPositionInLine}",
               e,
               e.offendingToken.line,
               e.offendingToken.charPositionInLine
            )
         }

         // Create a visitor to build the condition tree
         val visitor = FilterBuildingVisitor()

         // Visit the parse tree and return the condition
         try {
            val condition = visitor.visit(context.expression())
            return Filter(condition)
         } catch (e: DateTimeParseException) {
            throw DetailedParseException("Failed to parse date/time value '${e.message}'", e)
         } catch (e: RuntimeException) {
            throw DetailedParseException("Failed to parse filter '${e.message}'", e)
         }
      }
   }

   val rootCondition: Condition

   private constructor(rootCondition: Condition) {
      this.rootCondition = rootCondition
   }

   /**
    * Accepts a visitor to perform operations on the root condition of the Filter.
    *
    * @param visitor The visitor that will perform operations on the root condition.
    */
   fun accept(visitor: FilterVisitor) {
      visitor.visit(rootCondition)
   }

   /**
    * Executes the `compactPrint` operation, which generates a compact string representation of a `Filter`
    * object based on the provided `FilterPrintVisitor`.
    *
    * @return the compact string representation of the `Filter` object.
    */
   fun compactPrint(): String {
      val visitor = FilterPrintVisitor(FilterPrintVisitor.PrintFormat.COMPACT)
      accept(visitor)
      return visitor.print()
   }

   /**
    * Returns a formatted string representation of the Filter object.
    *
    * @return the pretty-printed string representation of the Filter object
    */
   fun prettyPrint(): String {
      val visitor = FilterPrintVisitor()
      accept(visitor)
      return visitor.print()
   }

   /**
    * Returns the string representation of the `Filter` object.
    *
    * This method internally calls the `compactPrint()` method to get the compact string representation of the `Filter`.
    *
    * @return the string representation of the `Filter` object
    */
   fun asString(): String {
      return compactPrint()
   }

   /**
    * Converts the given value to an HTTP query parameter string.
    *
    * @return the HTTP query parameter string
    */
   fun toHttpParameter(): String {
      return "$QUERY_PARAM$EQ${UriUtils.encodeQueryParam(asString(), StandardCharsets.UTF_8)}"
   }

   override fun toString(): String {
      return "Filter(rootCondition=$rootCondition)"
   }

}

private class FilterBuildingVisitor : FilterGrammarBaseVisitor<Filter.Condition>() {

   companion object {
      private val log: Logger = LoggerFactory.getLogger(FilterBuildingVisitor::class.java)
   }

   override fun visitLogicalExpression(ctx: FilterGrammarParser.LogicalExpressionContext): Filter.Condition {
      val left = visit(ctx.expression(0))
      val right = visit(ctx.expression(1))
      val operator = when (ctx.logicalOperator().text) {
         Filter.Operator.AND.representation -> Filter.Operator.AND
         Filter.Operator.OR.representation -> Filter.Operator.OR
         else -> throw DetailedParseException("Unknown logical operator ${ctx.logicalOperator().text}")
      }

      return Filter.Condition.ConditionGroup(operator).apply {
         addCondition(left)
         addCondition(right)
      }
   }

   override fun visitPredicateExpression(ctx: FilterGrammarParser.PredicateExpressionContext): Filter.Condition {
      return visit(ctx.predicate())
   }

   override fun visitParenExpression(ctx: FilterGrammarParser.ParenExpressionContext): Filter.Condition {
      return visit(ctx.expression())
   }

   override fun visitNotExpression(ctx: FilterGrammarParser.NotExpressionContext): Filter.Condition {
      val condition = visit(ctx.expression()) // This could be a parenthesized expression
      return Filter.Condition.NotCondition(condition)
   }

   override fun visitHavingPredicate(ctx: FilterGrammarParser.HavingPredicateContext): Filter.Condition {
      val field = ctx.field().text.trim()
      val subFilterString = ctx.expression().text.trim()
      val subFilter = Filter.of(subFilterString)

      return Filter.Condition.HavingCondition(field, subFilter)
   }

   override fun visitHavingFunctionPredicate(ctx: FilterGrammarParser.HavingFunctionPredicateContext): Filter.Condition {
      val field = ctx.field().text.trim()
      val function = Filter.Condition.HavingFunctionCondition.Function.valueOf(ctx.function().text.trim().uppercase())
      val operator = Filter.Operator.of(ctx.operator().text.trim())
      val value = createNumberValue(ctx.NUMBER().text.trim())
      return Filter.Condition.HavingFunctionCondition(field, function, operator, value)
   }

   override fun visitSimplePredicate(ctx: FilterGrammarParser.SimplePredicateContext): Filter.Condition {
      val field = ctx.field().text.trim()
      val operator = Filter.Operator.of(ctx.operator().text)
      val value = visitValue(ctx.value())

      return Filter.Condition.SimpleCondition(field, operator, value)
   }

   override fun visitNullPredicate(ctx: FilterGrammarParser.NullPredicateContext): Filter.Condition {
      val field = ctx.field().text.trim()
      return Filter.Condition.SimpleCondition(field, Filter.Operator.NULL, null)
   }

   override fun visitNotNullPredicate(ctx: FilterGrammarParser.NotNullPredicateContext): Filter.Condition {
      val field = ctx.field().text.trim()
      return Filter.Condition.SimpleCondition(field, Filter.Operator.NNULL, null)
   }

   override fun visitInListPredicate(ctx: FilterGrammarParser.InListPredicateContext): Filter.Condition {
      val field = ctx.field().text.trim()
      val values = ctx.valueList().children
         .filterIsInstance<FilterGrammarParser.ValueContext>()
         .map { visitValue(it) }
      return Filter.Condition.InListCondition(field, Filter.Operator.IN, values)
   }

   override fun visitNotInListPredicate(ctx: FilterGrammarParser.NotInListPredicateContext): Filter.Condition {
      val field = ctx.field().text.trim()
      val values = ctx.valueList().children
         .filterIsInstance<FilterGrammarParser.ValueContext>()
         .map { visitValue(it) }
      return Filter.Condition.NotInListCondition(field, Filter.Operator.NIN, values)
   }

   private fun visitValue(ctx: FilterGrammarParser.ValueContext, operator: Filter.Operator? = null): Filter.Value<*> {
      val parseMap = mapOf(
         FilterGrammarParser.NUMBER to ::createNumberValue,
         FilterGrammarParser.ESCAPED_STRING to { text -> createStringOrBooleanValue(operator, text) },
         FilterGrammarParser.DATETIME to ::createDatetimeValue,
         FilterGrammarParser.DATE to ::createDateValue,
         FilterGrammarParser.TIME to ::createTimeValue,
         FilterGrammarParser.MONTH_DAY to ::createMonthDayValue,
         FilterGrammarParser.YEAR to ::createYearValue
      )

      for ((tokenType, parseFunction) in parseMap) {
         ctx.getTokens(tokenType).firstOrNull()?.text?.trim()?.let { rawText ->
            return parseFunction(rawText)
         }
      }

      throw DetailedParseException("Unknown value type in $ctx")
   }

   private fun createNumberValue(raw: String): Filter.Value<*> =
      if (raw.contains(".")) {
         Filter.Value.DoubleValue(raw.toDouble())
      } else {
         Filter.Value.LongValue(raw.toLong())
      }

   private fun createStringOrBooleanValue(operator: Filter.Operator?, raw: String): Filter.Value<*> {
      raw.lowercase().let { lowercasedRaw ->
         if (lowercasedRaw == java.lang.Boolean.TRUE.toString() || lowercasedRaw == java.lang.Boolean.FALSE.toString()) {
            return Filter.Value.BooleanValue(lowercasedRaw.toBoolean())
         }
      }

      val unescapedRaw = Filter.resolveEscapes(raw)

      val hasNonEscapedWildcard = Filter.ESCAPED_WILDCARD_CHARS.any { char ->
         unescapedRaw.contains(Regex("(?<!\\\\)$char(?!\\\\)"))
      }

      // Determine the value based on the operator and presence of non-escaped wildcards
      val value = when {
         operator == Filter.Operator.LIKE && !hasNonEscapedWildcard -> "*$unescapedRaw*"
         else -> unescapedRaw
      }

      return Filter.Value.StringValue(value)
   }

   private fun createDateValue(raw: String): Filter.Value<*> =
      Filter.Value.DateValue(LocalDate.parse(raw))

   private fun createDatetimeValue(raw: String): Filter.Value<*> =
      if (raw.endsWith("Z")) {
         Filter.Value.UtcDateTimeValue(FlexibleInstantParser.parse(raw))
      } else {
         Filter.Value.DateTimeValue(LocalDateTime.parse(raw))
      }

   private fun createTimeValue(raw: String): Filter.Value<*> =
      Filter.Value.TimeValue(LocalTime.parse(raw))

   private fun createMonthDayValue(raw: String): Filter.Value<*> {
      val monthDayString = "--${raw.trimStart('-')}"
      return Filter.Value.MonthDayValue(MonthDay.parse(monthDayString))
   }

   private fun createYearValue(raw: String): Filter.Value<*> =
      Filter.Value.YearValue(raw.trimEnd('-').toInt())


}