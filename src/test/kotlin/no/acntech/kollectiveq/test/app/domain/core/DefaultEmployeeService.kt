package no.acntech.kollectiveq.test.app.domain.core

import no.acntech.kollectiveq.test.app.domain.api.EmployeeService
import no.acntech.kollectiveq.test.app.domain.model.Employee
import no.acntech.kollectiveq.test.app.domain.repository.EmployeeRepository
import org.springframework.stereotype.Service

@Service
class DefaultEmployeeService(
   repository: EmployeeRepository,
) : AbstractService<Employee>(repository), EmployeeService {
   override val repository: EmployeeRepository = repository
}