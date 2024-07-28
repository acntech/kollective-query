package no.acntech.kollectiveq.test.apptest.domain.repostitory

import jakarta.persistence.EntityManager
import jakarta.validation.Validation
import no.acntech.app.config.JpaTestConfig
import no.acntech.kollectiveq.test.app.domain.repository.DepartmentRepository
import no.acntech.kollectiveq.test.app.domain.repository.EmployeeRepository
import no.acntech.kollectiveq.test.app.domain.repository.ProjectRepository
import no.acntech.kollectiveq.test.apptest.BaseTest
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [JpaTestConfig::class])
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("default")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseRepositoryTest : BaseTest() {

   companion object {
      protected val VALIDATOR = Validation.buildDefaultValidatorFactory().validator
   }

   @Autowired
   protected lateinit var entityManager: EntityManager

   @Autowired
   protected lateinit var empRepo: EmployeeRepository

   @Autowired
   protected lateinit var deptRepo: DepartmentRepository

   @Autowired
   protected lateinit var projectRepo: ProjectRepository

}
