package no.acntech.kollectiveq

/**
 * Represents a filter visitor interface.
 */
interface FilterVisitor {
   fun visit(condition: Filter.Condition)
}