package no.acntech.kollectiveq.jpql

import jakarta.persistence.Query
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.EntityType
import jakarta.persistence.metamodel.Metamodel
import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.FilterBaseVisitor
import no.acntech.kollectiveq.collections.mapFromPairs
import no.acntech.kollectiveq.collections.prettyPrint
import no.acntech.kollectiveq.lang.TransformFunction
import no.acntech.kollectiveq.lang.identityTransform
import no.acntech.kollectiveq.persistence.PersistenceUtils
import no.acntech.kollectiveq.persistence.PersistenceUtils.getInverseAttribute
import no.acntech.kollectiveq.persistence.PersistenceUtils.getTargetEntityTypeForPluralAttribute
import no.acntech.kollectiveq.text.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.*
import java.util.*

/**
 * Visitor that transforms a [Filter] to a JPQL query.
 * <p>
 * @param entityName The name of the entity to filter on.
 * @param varName The name of the relationship attribute to use for the entity. Defaults to the first two
 * characters of the entity name in lowercase.
 * @param metamodel The JPA metamodel.
 * @param fieldTransformer A function that transforms a field name to a JPQL field name. Defaults to identity transform.
 * @param dbTimeZone The timezone to use for database queries. This is necessary due to differences in how databases handle
 * timezones. Defaults to UTC.
 */
class JPQLTransformationFilterVisitor : FilterBaseVisitor {

   companion object {
      private val log: Logger = LoggerFactory.getLogger(JPQLTransformationFilterVisitor::class.java)

      const val SINGLE_WILDCARD_CHAR = '_'
      const val MULTI_WILDCARD_CHAR = '%'

      const val QUOTE = "'"
      const val ESCAPED_QUOTE = "''"
   }

   private val parent: JPQLTransformationFilterVisitor?

   private val entityName: String

   private val varName: String

   private val metamodel: Metamodel

   private val fieldTransformer: TransformFunction<String, String>

   private val entityType: EntityType<*>

   private var nestingAllowed: Boolean = true

   private val variables: MutableSet<String>

   private val parameters: MutableMap<String, Any>

   private val sb = StringBuilder()

   private var depth = 0 // Add depth field to handle nested conditions

   constructor(
      entityName: String,
      varName: String = entityName.take(2).lowercase() + UNDERSCORE + "0",
      metamodel: Metamodel,
      fieldTransformer: TransformFunction<String, String> = identityTransform(),
   ) : this(null, entityName, varName, metamodel, fieldTransformer, true)

   private constructor(
      parent: JPQLTransformationFilterVisitor?,
      entityName: String,
      varName: String,
      metamodel: Metamodel,
      fieldTransformer: TransformFunction<String, String>,
      nestingAllowed: Boolean,
   ) : super() {
      entityType = PersistenceUtils.getEntityType(metamodel, entityName)
         ?: throw DetailedParseException("Entity type not found for entity name '$entityName'")

      this.parent = parent
      this.entityName = entityName
      this.varName = varName
      this.metamodel = metamodel
      this.fieldTransformer = fieldTransformer
      this.nestingAllowed = nestingAllowed

      // Set the parameters and variables from the root parent
      val rootParent: JPQLTransformationFilterVisitor? = generateSequence(parent) { it.parent }.lastOrNull()
      this.parameters = rootParent?.parameters ?: mutableMapOf()
      this.variables = rootParent?.variables ?: mutableSetOf()

      variables.add(varName)

      log.trace("Created JPQLTransformationFilterVisitor: $this")
   }

   fun setParameters(query: Query) {
      log.trace("Setting query parameters for query '$query': params$NEW_LINE${parameters.prettyPrint()}")
      parameters.forEach { (name, value) ->
         query.setParameter(name, value)
      }
   }

   override fun visitConditionGroup(conditionGroup: Filter.Condition.ConditionGroup) {
      if (depth > 0) {
         sb.append(SPACE)
      }
      sb.append(LEFT_PAREN)
      depth++ // Increase depth because we're entering a new group

      val iterator = conditionGroup.conditions.iterator()
      while (iterator.hasNext()) {
         visit(iterator.next()) // Visit the condition
         if (iterator.hasNext()) {
            // Append the logical operator if there are more conditions
            sb.append(" ${translateOperator(conditionGroup.operator)} ")
         }
      }

      depth-- // Decrease depth because we're leaving a group
      sb.append(RIGHT_PAREN)

      if (depth > 0) {
         sb.append(SPACE)
      }
   }

   override fun visitHavingCondition(condition: Filter.Condition.HavingCondition) {
      validateAndPrepareAttribute(condition.field) {
            transformedField,
            attribute,
            targetEntityType,
            targetVarName,
            inverseAttributeName,
         -> // Function parameter
         handleAttribute(
            transformedField,
            attribute,
            targetEntityType,
            targetVarName,
            inverseAttributeName,
            condition.subFilter
         )
      }
   }

   /**
    * Example 1:
    *
    * $having:MIN(employees.salary)$gte:10000 =>
    *
    * SELECT d FROM Department d
    * WHERE d.id IN (
    *     SELECT e.department.id
    *     FROM Employee e
    *     GROUP BY e.department.id
    *     HAVING MIN(e.salary) >= 10000
    * )
    *
    * Example 2:
    *
    * $having:COUNT(employees)$gte:10 =>
    *
    * SELECT d FROM Department d
    * WHERE d.id IN (
    *     SELECT e.department.id
    *     FROM Employee e
    *     GROUP BY e.department.id
    *     HAVING COUNT(e) >= 10000
    * )
    */
   override fun visitHavingFunctionCondition(condition: Filter.Condition.HavingFunctionCondition) {
      validateAndPrepareAttribute(condition.field) {
            transformedField,
            attribute,
            targetEntityType,
            targetVarName,
            inverseAttributeName,
         -> // Function parameter
         handleFunctionAttribute(
            transformedField,
            attribute,
            targetEntityType,
            targetVarName,
            inverseAttributeName,
            condition
         )
      }
   }

   override fun visitSimpleCondition(condition: Filter.Condition.SimpleCondition) {
      val field = transformAndCheckField(condition.field)
      val qualifiedField = qualifyField(field)
      val operator = translateOperator(condition.operator)

      if (condition.operator == Filter.Operator.NULL || condition.operator == Filter.Operator.NNULL) {
         sb.append("$qualifiedField $operator")
      } else {
         val value = translateValue(field, qualifiedField, condition.operator, condition.value)

         val toAppend: String =
            when (condition.value) {
               is Filter.Value.StringValue -> "LOWER($qualifiedField) $operator $value"
               is Filter.Value.TimeValue -> value
               is Filter.Value.MonthDayValue -> value
               is Filter.Value.YearValue -> value
               else -> "$qualifiedField $operator $value"
            }

         sb.append(toAppend)
      }
   }

   override fun visitInListCondition(condition: Filter.Condition.InListCondition) {
      val qualifiedField = qualifyField(transformAndCheckField(condition.field))
      val values = condition.values.joinToString(separator = COMMA) { translateListValue(it) }
      sb.append("$qualifiedField IN ($values)")
   }

   override fun visitNotInListCondition(condition: Filter.Condition.NotInListCondition) {
      val qualifiedField = qualifyField(transformAndCheckField(condition.field))
      val values = condition.values.joinToString(separator = COMMA) { translateListValue(it) }
      sb.append("$qualifiedField NOT IN ($values)")
   }

   override fun visitNotCondition(condition: Filter.Condition.NotCondition) {
      sb.append("NOT ")
      visit(condition.condition)
   }

   fun toWhereClause(): String {
      return sb.toString()
   }

   fun toQuery(): String {
      val whereClause = toWhereClause()
      return "SELECT $varName FROM $entityName $varName" +
         if (whereClause.isNotBlank()) " WHERE $whereClause" else EMPTY_STRING
   }

   fun toCountQuery(): String {
      val whereClause = toWhereClause()
      return "SELECT COUNT($varName) FROM $entityName $varName" +
         if (whereClause.isNotBlank()) " WHERE $whereClause" else EMPTY_STRING
   }

   override fun toString(): String {
      return ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
         .append(this::entityName.name, entityName)
         .append(this::entityType.name, entityType)
         .append(this::varName.name, varName)
         .append(this::parent.name, parent?.entityName)
         .append(this::variables.name, variables)
         .append(this::parameters.name, parameters)
         .append(this::sb.name, sb)
         .toString()
   }

   private fun validateAndPrepareAttribute(
      field: String,
      function: (String, Attribute<*, *>, EntityType<*>, String, String) -> Unit, // Function parameter
   ) {
      if (!nestingAllowed) {
         throw DetailedParseException("Condition is not allowed in a nested context ($field)")
      }

      val transformedField = transformAndCheckField(field).also {
         log.trace("Transformed field: $it")
      }
      val parts = transformedField.split('.')
      var attribute: Attribute<*, *>

      for (i in parts.size downTo 1) {
         val subField = parts.subList(0, i).joinToString(PERIOD)
         attribute = getAttributeOrThrow(subField).also {
            log.trace("Attribute found: ${it.name} of type ${it.javaType}")
         }

         if (attribute.isCollection) {
            val targetEntityType = getTargetEntityTypeForPluralAttributeOrThrow(metamodel, attribute)
            val targetVarName = generateVariableName(targetEntityType.name)
            val inverseAttributeName = getInverseAttribute(entityType, attribute, targetEntityType).name
            function(transformedField, attribute, targetEntityType, targetVarName, inverseAttributeName)
            return
         }
      }

      throw DetailedParseException("The condition does not support non-collection attributes: $transformedField")
   }

   private fun getAttributeOrThrow(field: String): Attribute<*, *> =
      try {
         PersistenceUtils.findAttributeByPath(entityType, field).also {
            log.trace("Attribute found: '$entityName.$field': ${it.name} of type ${it.javaType}")
         }
      } catch (e: IllegalArgumentException) {
         throw DetailedParseException("No attribute '$field' defined for entity '$entityName'")
      }

   private fun getTargetEntityTypeForPluralAttributeOrThrow(metamodel: Metamodel, attribute: Attribute<*, *>): EntityType<*> {
      return try {
         getTargetEntityTypeForPluralAttribute(metamodel, attribute)
      } catch (e: IllegalArgumentException) {
         throw DetailedParseException("Could not determine target entity type for attribute '$attribute' - error: ${e.message}")
      }
   }

   /*
    * $having: clause
    */
   private fun handleAttribute(
      field: String,
      attribute: Attribute<*, *>,
      targetEntityType: EntityType<*>,
      targetVarName: String,
      inverseAttributeName: String,
      subFilter: Filter,
   ) {
      // Delegate to the sub-filter to apply its conditions within the sub-query
      val subVisitor = JPQLTransformationFilterVisitor(
         this,
         targetEntityType.name,
         targetVarName,
         metamodel,
         fieldTransformer,
//         false
         true
      )
      subFilter.accept(subVisitor)

      // Extract the WHERE clause from the sub-query
      val subWhereClause = subVisitor.toWhereClause()

      // Construct the sub-query based on the relationship
      sb.append("$varName IN $LEFT_PAREN")
      sb.append("SELECT $targetVarName$DOT$inverseAttributeName FROM ${targetEntityType.name} $targetVarName")

      if (subWhereClause.isNotEmpty()) { // Append the WHERE clause if it's not empty
         sb.append(" WHERE $subWhereClause")
      }

      // Close the IN sub-query
      sb.append(RIGHT_PAREN)
   }

   /*
    * $having:FUNCTION clause
    */
   private fun handleFunctionAttribute(
      field: String,
      attribute: Attribute<*, *>,
      targetEntityType: EntityType<*>,
      targetVarName: String,
      inverseAttributeName: String,
      condition: Filter.Condition.HavingFunctionCondition,
   ) {
      if (field.count { it == '.' } > 1) {
         throw DetailedParseException("HavingFunctionCondition does not support multi-nested fields: $field")
      }

      var functionAttributeName = field.substringAfterLast(DOT).takeIf { field.contains(DOT) } ?: EMPTY_STRING

      val function = condition.function

      if (functionAttributeName.isEmpty() && function != Filter.Condition.HavingFunctionCondition.Function.COUNT) {
         throw DetailedParseException("HavingFunctionCondition requires a field path for function '${function.name}($field)'")
      }

      val ourIdAttributeName = PersistenceUtils.getIdAttributeName(entityType)

      if (functionAttributeName.isEmpty()) {
         functionAttributeName = PersistenceUtils.getIdAttributeName(targetEntityType)
      }

      sb.append("$varName$DOT$ourIdAttributeName IN $LEFT_PAREN")
      sb.append(
         "SELECT $targetVarName$DOT$inverseAttributeName$DOT$ourIdAttributeName" +
            " FROM ${targetEntityType.name} $targetVarName" +
            " GROUP BY $targetVarName$DOT$inverseAttributeName$DOT$ourIdAttributeName" +
            " HAVING ${function.name}$LEFT_PAREN" +
            "$targetVarName$DOT$functionAttributeName) ${
               translateOperator(
                  condition.operator
               )
            } ${condition.value.value.toString()}"
      )

      // Close the IN sub-query
      sb.append(RIGHT_PAREN)
   }

   private fun transformAndCheckField(field: String): String {
      val transformedField: String = fieldTransformer(field)

      checkLegalField(transformedField)

      return transformedField.also {
         log.trace("Field '$field' transformed to '$it'")
      }
   }

   private fun qualifyField(field: String): String {
      return "$varName$DOT$field"
   }

   private fun translateOperator(operator: Filter.Operator): String {
      return when (operator) {
         Filter.Operator.EQ -> "="
         Filter.Operator.NE -> "<>"
         Filter.Operator.GT -> ">"
         Filter.Operator.GTE -> ">="
         Filter.Operator.LT -> "<"
         Filter.Operator.LTE -> "<="
         Filter.Operator.LIKE -> "LIKE"
         Filter.Operator.AND -> "AND"
         Filter.Operator.OR -> "OR"
         Filter.Operator.NOT -> "NOT"
         Filter.Operator.NULL -> "IS NULL"
         Filter.Operator.NNULL -> "IS NOT NULL"
         Filter.Operator.IN -> "IN"
         Filter.Operator.NIN -> "NOT IN"
      }
   }

   private fun translateValue(
      field: String,
      qualifiedField: String,
      operator: Filter.Operator,
      value: Filter.Value<*>?,
   ): String {

      if (log.isTraceEnabled) {
         val params: Map<Any, Any> = mapFromPairs(
            "field", field,
            "qualifiedField", qualifiedField,
            "operator", operator,
            "value", value
         )
         log.trace("Translating value:\n${params.prettyPrint()}")
      }

      val result = when (value) {
         is Filter.Value.StringValue -> translateStringValue(field, qualifiedField, operator, value)
         is Filter.Value.LongValue -> translateLongValue(field, value)
         is Filter.Value.DoubleValue -> translateDoubleValue(field, value)
         is Filter.Value.BooleanValue -> translateBooleanValue(field, operator, value)
         is Filter.Value.TimeValue -> translateTimeValue(qualifiedField, operator, value)
         is Filter.Value.DateValue -> translateDateValue(value)
         is Filter.Value.DateTimeValue -> translateDateTimeValue(value)
         is Filter.Value.UtcDateTimeValue -> translateUtcDateTimeValue(field, value)
         is Filter.Value.MonthDayValue -> translateMonthDayValue(qualifiedField, operator, value)
         is Filter.Value.YearValue -> translateYearValue(qualifiedField, operator, value)
         null -> throw DetailedParseException("Illegal null value for field '$field'")
      }

      log.trace("Field: $qualifiedField, operator: $operator, value: $value -> value: $result")

      return result
   }

   private fun translateBooleanValue(field: String, operator: Filter.Operator, value: Filter.Value.BooleanValue): String {
      if (operator != Filter.Operator.EQ && operator != Filter.Operator.NE) {
         throw DetailedParseException(
            "Illegal operator '$operator' for Boolean value '$value'" +
               " - only '${Filter.Operator.EQ}' and '${Filter.Operator.NE}' are supported"
         )
      }

      val attribute = getAttributeOrThrow(field)
      val booleanValue = value.value

      // Check if the attribute is a boolean type
      return if (PersistenceUtils.isBooleanType(attribute)) {
         // If it's a boolean, return a parameter placeholder
         COLON + generateParameterName(attribute).apply { parameters[this] = booleanValue }
      } else {
         // If it's not a boolean (e.g. String), return the value enclosed in single quotes
         "'$value'"
      }
   }

   private fun translateMonthDayValue(
      qualifiedField: String,
      operator: Filter.Operator,
      value: Filter.Value.MonthDayValue,
   ): String {
      val month = value.value.monthValue
      val day = value.value.dayOfMonth

      return when (operator) {

         Filter.Operator.EQ ->
            "(MONTH($qualifiedField) = $month AND DAY($qualifiedField) = $day)"

         Filter.Operator.NE ->
            "(MONTH($qualifiedField) != $month OR DAY($qualifiedField) != $day)"

         Filter.Operator.GT ->
            "(MONTH($qualifiedField) > $month OR " +
               "(MONTH($qualifiedField) = $month AND DAY($qualifiedField) > $day))"

         Filter.Operator.GTE ->
            "(MONTH($qualifiedField) > $month OR " +
               "(MONTH($qualifiedField) = $month AND DAY($qualifiedField) >= $day))"

         Filter.Operator.LT ->
            "(MONTH($qualifiedField) < $month OR " +
               "(MONTH($qualifiedField) = $month AND DAY($qualifiedField) < $day))"

         Filter.Operator.LTE ->
            "(MONTH($qualifiedField) < $month OR " +
               "(MONTH($qualifiedField) = $month AND DAY($qualifiedField) <= $day))"

         else -> throw DetailedParseException("Illegal operator for MonthDay value '$value': $operator")
      }
   }

   private fun translateYearValue(
      qualifiedField: String,
      operator: Filter.Operator,
      value: Filter.Value.YearValue,
   ): String {
      val year = value.value

      return when (operator) {
         Filter.Operator.EQ ->
            "YEAR($qualifiedField) = $year"

         Filter.Operator.NE ->
            "YEAR($qualifiedField) != $year"

         Filter.Operator.GT ->
            "YEAR($qualifiedField) > $year"

         Filter.Operator.GTE ->
            "YEAR($qualifiedField) >= $year"

         Filter.Operator.LT ->
            "YEAR($qualifiedField) < $year"

         Filter.Operator.LTE ->
            "YEAR($qualifiedField) <= $year"

         else -> throw DetailedParseException("Illegal operator for Year value '$value': $operator")
      }
   }

   private fun translateUtcDateTimeValue(field: String, value: Filter.Value.UtcDateTimeValue): String {
      val attribute = getAttributeOrThrow(field)
      val instant = value.value

      val javaType = attribute.javaType
      val timeValue = when {
         javaType.isAssignableFrom(Instant::class.java) -> instant

         javaType.isAssignableFrom(OffsetDateTime::class.java) -> instant.atOffset(ZoneOffset.UTC)

         javaType.isAssignableFrom(ZonedDateTime::class.java) -> instant.atZone(ZoneId.systemDefault())

         javaType.isAssignableFrom(Date::class.java) -> Date.from(instant)

         javaType.isAssignableFrom(LocalDateTime::class.java) -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

         javaType.isAssignableFrom(LocalDate::class.java) -> LocalDateTime.ofInstant(
            instant,
            ZoneId.systemDefault()
         ).toLocalDate()

         javaType.isAssignableFrom(LocalTime::class.java) -> LocalDateTime.ofInstant(
            instant,
            ZoneId.systemDefault()
         ).toLocalTime()

         javaType.isAssignableFrom(OffsetTime::class.java) -> LocalDateTime.ofInstant(
            instant,
            ZoneId.systemDefault()
         ).atOffset(ZoneOffset.UTC).toOffsetTime()

         javaType.isAssignableFrom(Timestamp::class.java) -> Timestamp.from(instant)

         else -> throw DetailedParseException(
            "Cannot assign time value '$instant' to attribute " +
               "'$entityName.${attribute.name}' of type '${javaType.name}'"
         )
      }

      return COLON + generateParameterName(attribute).apply { parameters[this] = timeValue }
   }

   private fun translateTimeValue(
      qualifiedField: String,
      operator: Filter.Operator,
      value: Filter.Value.TimeValue,
   ): String {
      val hour: Int = value.value.hour
      val minute: Int = value.value.minute
      val second: Int = value.value.second

      return when (operator) {
         Filter.Operator.EQ ->
            "(HOUR($qualifiedField) = $hour AND " +
               "MINUTE($qualifiedField) = $minute AND " +
               "SECOND($qualifiedField) = $second)"

         Filter.Operator.NE ->
            "(HOUR($qualifiedField) != $hour OR " +
               "MINUTE($qualifiedField) != $minute OR " +
               "SECOND($qualifiedField) != $second)"

         Filter.Operator.GT ->
            "(HOUR($qualifiedField) > $hour OR " +
               "(HOUR($qualifiedField) = $hour AND " +
               "(MINUTE($qualifiedField) > $minute OR " +
               "(MINUTE($qualifiedField) = $minute AND " +
               "SECOND($qualifiedField) > $second))))"

         Filter.Operator.GTE ->
            "(HOUR($qualifiedField) > $hour OR " +
               "(HOUR($qualifiedField) = $hour AND " +
               "(MINUTE($qualifiedField) > $minute OR " +
               "(MINUTE($qualifiedField) = $minute AND " +
               "SECOND($qualifiedField) >= $second))))"

         Filter.Operator.LT ->
            "(HOUR($qualifiedField) < $hour OR " +
               "(HOUR($qualifiedField) = $hour AND " +
               "(MINUTE($qualifiedField) < $minute OR " +
               "(MINUTE($qualifiedField) = $minute AND " +
               "SECOND($qualifiedField) < $second))))"

         Filter.Operator.LTE ->
            "(HOUR($qualifiedField) < $hour OR " +
               "(HOUR($qualifiedField) = $hour AND " +
               "(MINUTE($qualifiedField) < $minute OR " +
               "(MINUTE($qualifiedField) = $minute AND " +
               "SECOND($qualifiedField) <= $second))))"

         else -> throw DetailedParseException("Illegal operator for Time value '$value': $operator")
      }
   }

   private fun translateStringValue(
      field: String,
      qualifiedField: String,
      operator: Filter.Operator,
      value: Filter.Value.StringValue,
   ): String {
      logTraceBeforeTranslation(qualifiedField, operator, value)

      val jpqlValue = when (operator) {
         Filter.Operator.LIKE -> formatJpqlValue(escapeForLikeOperator(value.value))
         else -> formatJpqlValue(escapeSingleQuotes(value.value))
      }

      return jpqlValue.also { logTraceAfterTranslation(it) }
   }

   private fun escapeForLikeOperator(value: String): String {
      val result = value
         .replace("\\", "\\\\")
         .replace("%", "\\%")
         .replace("_", "\\_")
         .replace(Regex("(?<!\\*)\\*(?!\\*)"), "%")
         .replace(Regex("(?<!\\?)\\?(?!\\?)"), "_")
         .replace("**", "*")
         .replace("??", "?")

      return escapeSingleQuotes(result)
   }

   private fun escapeSingleQuotes(value: String): String = value.replace("'", "''")

   private fun formatJpqlValue(value: String): String = "LOWER('$value')"

   private fun logTraceBeforeTranslation(qualifiedField: String, operator: Filter.Operator, value: Filter.Value.StringValue) {
      log.trace("JPQL translateStringValue field '$qualifiedField', operator '$operator', value '$value'")
   }

   private fun logTraceAfterTranslation(jpqlValue: String) {
      log.trace("JPQL value: $jpqlValue")
   }

   private fun translateLongValue(field: String, value: Filter.Value.LongValue): String =
      translateNumberValue(field, value.value)

   private fun translateDoubleValue(field: String, value: Filter.Value.DoubleValue): String =
      translateNumberValue(field, value.value)

   private fun translateNumberValue(field: String, value: Number): String {
      // Find the JPA attribute for the given field path
      val attribute = getAttributeOrThrow(field)

      // Check if the attribute is a number type
      return if (PersistenceUtils.isNumberType(attribute)) {
         // If it's a number, return the value as is
         value.toString()
      } else {
         // If it's not a number (e.g., String), return the value enclosed in single quotes
         "'$value'"
      }
   }

   private fun translateDateTimeValue(value: Filter.Value.DateTimeValue) = "CAST('${value.value}' AS TIMESTAMP)"

   private fun translateDateValue(value: Filter.Value.DateValue) = "CAST('${value.value}' AS DATE)"

   private fun translateListValue(it: Filter.Value<*>): String {
      return when (it) {
         is Filter.Value.StringValue -> "'${it.value}'"
         is Filter.Value.LongValue -> it.value.toString()
         is Filter.Value.DoubleValue -> it.value.toString()
         else -> throw DetailedParseException("Illegal list value: $it")
      }
   }

   private fun checkLegalField(field: String) {
      if (!PersistenceUtils.isValidPath(entityType, field)) {
         throw DetailedParseException("Illegal field or path '$field' for entity '$entityName'")
      }
   }

   private fun generateVariableName(entity: String): String {
      val varNamePrefix = entity.take(2).lowercase() + UNDERSCORE
      var index = 0

      while (true) {
         val varName = "$varNamePrefix$index"
         if (varName !in variables) {
            return varName
         }
         index++
      }
   }

   private fun generateParameterName(attribute: Attribute<*, *>): String {
      val paramNamePrefix = attribute.name + UNDERSCORE
      var index = 0

      while (true) {
         val paramName = "$paramNamePrefix$index"
         if (paramName !in parameters.keys) {
            return paramName
         }
         index++
      }
   }

}