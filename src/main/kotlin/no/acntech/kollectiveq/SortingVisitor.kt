package no.acntech.kollectiveq

/**
 * Interface for a visitor that visits sorting criteria.
 */
interface SortingVisitor {

   fun visitCriteria(criteria: List<Sorting.SortCriterion>) {
      criteria.forEach { visitCriterion(it) }
   }

   fun visitCriterion(criterion: Sorting.SortCriterion)
}