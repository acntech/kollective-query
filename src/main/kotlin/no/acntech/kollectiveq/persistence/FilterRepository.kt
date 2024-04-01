package no.acntech.kollectiveq.persistence

import com.google.common.base.CaseFormat
import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.Pagination
import no.acntech.kollectiveq.Sorting
import no.acntech.kollectiveq.util.lang.TransformFunction
import no.acntech.kollectiveq.util.lang.createCaseFormatTransformFunction
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.io.Serializable

/**
 * Interface for filter repositories that extend JpaRepository. This interface provides a method for filtering entities. Use
 * this interface as a base interface for your repositories that need to filter entities.
 * <p>
 * Add a Spring configuration class that enables JPA repositories and sets the repositoryBaseClass to DefaultFilterRepository.
 * <p>
 * Example:
 * <pre><code>
 * @Configuration
 * @EntityScan(basePackages = ["package.to.scan.for.entities"])
 * @EnableJpaRepositories(
 *    basePackages = [
 *       "no.acntech.kollectiveq.persistence",
 *       "package.to.scan.for.repositories"
 *    ],
 *    repositoryBaseClass = DefaultFilterRepository::class
 * )
 * open class JpaConfig
 * </code></pre>
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