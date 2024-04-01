package no.acntech.kollectiveq.test.app.config

import no.acntech.kollectiveq.persistence.DefaultFilterRepository
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(basePackages = ["no.acntech.kollectiveq.test.app.domain.model"])
@EnableJpaRepositories(
   basePackages = [
      "no.acntech.kollectiveq.persistence",
      "no.acntech.kollectiveq.test.app.domain.repository"
   ],
   repositoryBaseClass = DefaultFilterRepository::class
)
open class JpaConfig