package no.acntech.kollectiveq.test.app.controller

import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.Pagination
import no.acntech.kollectiveq.Sorting
import no.acntech.kollectiveq.test.app.domain.api.EmployeeService
import no.acntech.kollectiveq.test.app.domain.model.Employee
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employees")
class EmployeeController(
   private val employeeService: EmployeeService,
) {
   @GetMapping
   fun getAll(
      @RequestParam(name = "pagination", required = false) pagination: Pagination?,
      @RequestParam(name = "filter", required = false) filter: Filter?,
      @RequestParam(name = "sort", required = false) sorting: Sorting?,
   ): ResponseEntity<List<Employee>> {
      val effectivePagination = pagination ?: Pagination()
      val users = employeeService.getEntities(effectivePagination, filter, sorting)
      return ResponseEntity.ok(users.content)
   }

}
