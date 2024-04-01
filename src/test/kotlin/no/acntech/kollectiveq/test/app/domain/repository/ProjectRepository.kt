package no.acntech.kollectiveq.test.app.domain.repository

import no.acntech.kollectiveq.persistence.FilterRepository
import no.acntech.kollectiveq.test.app.domain.model.Project
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : FilterRepository<Project, Long>
