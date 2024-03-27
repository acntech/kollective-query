package no.acntech.kollectiveq.persistence

import com.google.common.base.CaseFormat
import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.Pagination
import no.acntech.kollectiveq.Sorting
import no.acntech.kollectiveq.lang.TransformFunction
import no.acntech.kollectiveq.lang.createCaseFormatTransformFunction
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.io.Serializable

/**
 * Interface for filter repositories that extend JpaRepository.
 *
 * @param E the entity type
 * @param ID the ID type of the entity
 */
@NoRepositoryBean
interface FilterRepository<E, ID : Serializable> : JpaRepository<E, ID> {

   fun getEntities(
      pagination: Pagination,
      filter: Filter?,
      sorting: Sorting?,
      fieldTransformer: TransformFunction<String, String> =
         createCaseFormatTransformFunction(
            CaseFormat.LOWER_UNDERSCORE,
            CaseFormat.LOWER_CAMEL
         ),
   ): Page<E>

}