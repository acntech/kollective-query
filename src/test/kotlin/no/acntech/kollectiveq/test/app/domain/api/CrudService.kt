package no.acntech.kollectiveq.test.app.domain.api

import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.Pagination
import no.acntech.kollectiveq.Sorting
import org.springframework.data.domain.Page

interface CrudService<E> {

   /**
    * Extract a collection of entities from the underlying repository based on the provided pagination, filter and sort criteria.
    */
   fun getEntities(pagination: Pagination?, filter: Filter?, sort: Sorting?): Page<E>

   fun getEntityById(id: Long): E?

   fun createEntity(entity: E): E

   fun updateEntity(id: Long, entity: E): E

   fun deleteEntityById(id: Long): Boolean
}
