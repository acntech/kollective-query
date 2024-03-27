package no.acntech.kollectiveq.test.app.domain.repository

import no.acntech.kollectiveq.persistence.FilterRepository
import no.acntech.kollectiveq.test.app.domain.model.Department
import org.springframework.stereotype.Repository

@Repository
interface DepartmentRepository : FilterRepository<Department, Long>
