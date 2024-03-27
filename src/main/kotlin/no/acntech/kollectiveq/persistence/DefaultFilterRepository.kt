package no.acntech.kollectiveq.persistence

import jakarta.persistence.EntityManager
import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.Pagination
import no.acntech.kollectiveq.Sorting
import no.acntech.kollectiveq.jpql.JPQLTransformationFilterVisitor
import no.acntech.kollectiveq.jpql.JPQLTransformationSortingVisitor
import no.acntech.kollectiveq.lang.TransformFunction
import no.acntech.kollectiveq.lang.snakeToCamelTransformer
import no.acntech.kollectiveq.text.EMPTY_STRING
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import java.io.Serializable

class DefaultFilterRepository<T : Any, ID : Serializable>(
   entityInformation: JpaEntityInformation<T, ID>,
   private val entityManager: EntityManager,
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager), FilterRepository<T, ID> {

   private data class JPQLQueries(
      val jpqlQuery: String,
      val jpqlCountQuery: String,
      val visitor: JPQLTransformationFilterVisitor?,
   )

   companion object {
      private val log = LoggerFactory.getLogger(DefaultFilterRepository::class.java)
   }

   private var entityClass: Class<T> = entityInformation.javaType

   private var entityName: String = entityClass.simpleName

   private var legalFields: Set<String> = PersistenceUtils.getEntityFields(entityManager.metamodel, entityClass)

   private var relationshipAttributeName: String = entityName.take(1).lowercase()

   private var defaultQuery: String = "SELECT e FROM $entityName e"

   private var defaultCountQuery: String = "SELECT COUNT(e) FROM $entityName e"

   override fun getEntities(
      pagination: Pagination,
      filter: Filter?,
      sorting: Sorting?,
      fieldTransformer: TransformFunction<String, String>,
   ): Page<T> {
      val jpqlQueries = constructJPQLQueries(filter, sorting, fieldTransformer)

      logQueryDetails(pagination, filter, sorting, jpqlQueries)

      val count = executeCountQuery(jpqlQueries.jpqlCountQuery, jpqlQueries.visitor)

      val result = executeMainQuery(pagination, jpqlQueries)

      return PageImpl(result, PageRequest.of(pagination.page - 1, pagination.size), count)
   }

   private fun constructJPQLQueries(
      filter: Filter?,
      sorting: Sorting?,
      fieldTransformer: TransformFunction<String, String>,
   ): JPQLQueries {
      var jpqlCountQuery = defaultCountQuery

      val visitor = filter?.let {
         JPQLTransformationFilterVisitor(
            metamodel = entityManager.metamodel,
            entityName = entityName,
            varName = relationshipAttributeName,
            fieldTransformer = snakeToCamelTransformer
         )
      }

      val jpqlQuery = filter?.let {
         filter.accept(visitor!!)
         jpqlCountQuery = visitor.toCountQuery()
         visitor.toQuery()
      } ?: defaultQuery

      val sortClause = sorting?.let {
         val sortingVisitor = JPQLTransformationSortingVisitor(fieldTransformer, legalFields)
         sorting.accept(sortingVisitor)
         sortingVisitor.toSortingClause()
      }

      val fullQuery = "$jpqlQuery ${
         sortClause.takeUnless { it.isNullOrBlank() }?.let { "ORDER BY $it" } ?: EMPTY_STRING
      }"

      return JPQLQueries(fullQuery, jpqlCountQuery, visitor)
   }

   private fun logQueryDetails(
      pagination: Pagination,
      filter: Filter?,
      sorting: Sorting?,
      jpqlQueries: JPQLQueries,
   ) {
      log.trace(
         "DefaultFilterRepository.getEntities:" +
            "\n\tPagination:       $pagination" +
            "\n\tFilter:           $filter" +
            "\n\tSorting:          $sorting" +
            "\n\tJPQL query:       ${jpqlQueries.jpqlQuery}" +
            "\n\tJPQL count-query: ${jpqlQueries.jpqlCountQuery}"
      )
   }

   private fun executeCountQuery(
      jpqlCountQuery: String,
      visitor: JPQLTransformationFilterVisitor?,
   ): Long {
      val countQuery = entityManager.createQuery(jpqlCountQuery, Long::class.java)
      visitor?.setParameters(countQuery)

      return countQuery.singleResult.also { log.trace("count: $it") }
   }

   private fun executeMainQuery(
      pagination: Pagination,
      jpqlQueries: JPQLQueries,
   ): List<T> {
      val query = entityManager.createQuery(jpqlQueries.jpqlQuery, entityClass)
      jpqlQueries.visitor?.setParameters(query)

      query.firstResult = pagination.startIndex
      query.maxResults = pagination.size

      return query.resultList.also { log.trace("result: $it") }
   }

}