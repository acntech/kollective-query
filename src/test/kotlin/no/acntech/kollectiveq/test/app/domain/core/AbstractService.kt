package no.acntech.kollectiveq.test.app.domain.core

import jakarta.persistence.EntityNotFoundException
import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.Pagination
import no.acntech.kollectiveq.Sorting
import no.acntech.kollectiveq.persistence.FilterRepository
import no.acntech.kollectiveq.test.app.domain.api.CrudService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page

abstract class AbstractService<E : Any>(
   protected open val repository: FilterRepository<E, Long>,
) : CrudService<E> {

   protected var log: Logger = LoggerFactory.getLogger(javaClass)

   override fun getEntities(pagination: Pagination?, filter: Filter?, sort: Sorting?): Page<E> {
      return repository.getEntities(pagination ?: Pagination(), filter, sort)
   }

   override fun getEntityById(id: Long): E? {
      return repository.findById(id).orElse(null)
   }

   override fun createEntity(entity: E): E {
      return repository.save(entity)
   }

   override fun updateEntity(id: Long, entity: E): E {
      return if (repository.existsById(id)) {
         repository.save(entity)
      } else {
         throw EntityNotFoundException("Entity not found with id: $id")
      }
   }

   override fun deleteEntityById(id: Long): Boolean {
      return if (repository.existsById(id)) {
         repository.deleteById(id)
         true
      } else {
         false
      }
   }

}
