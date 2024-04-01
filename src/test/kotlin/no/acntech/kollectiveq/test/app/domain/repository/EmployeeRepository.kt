package no.acntech.kollectiveq.test.app.domain.repository

import no.acntech.kollectiveq.persistence.FilterRepository
import no.acntech.kollectiveq.test.app.domain.model.Employee
import org.springframework.stereotype.Repository

@Repository
interface EmployeeRepository : FilterRepository<Employee, Long>
