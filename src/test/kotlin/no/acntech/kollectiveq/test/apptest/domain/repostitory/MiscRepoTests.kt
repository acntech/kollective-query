package no.acntech.app.domain.repostitory

import jakarta.persistence.TypedQuery
import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.Pagination
import no.acntech.kollectiveq.Sorting
import no.acntech.kollectiveq.jpql.JPQLTransformationFilterVisitor
import no.acntech.kollectiveq.test.app.domain.model.Department
import no.acntech.kollectiveq.test.app.domain.model.Employee
import no.acntech.kollectiveq.test.apptest.domain.repostitory.BaseRepositoryTest
import no.acntech.kollectiveq.util.collections.prettyPrint
import no.acntech.kollectiveq.util.lang.snakeToCamelTransformer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.Page

class MiscRepoTests : BaseRepositoryTest() {

   @BeforeAll
   fun initAll() {
      log.info("@BeforeAll")
   }

   @AfterAll
   fun tearDownAll() {
      log.info("@AfterAll")
   }

   @AfterEach
   fun tearDown() {
      log.info("@AfterEach")
   }

//    @Test
//    fun testComplexQuery() {
//        val input =
//            "first_name\$like:J*n?\$and:last_name\$like:Jo?n*son\$and:last_login\$lt:2023-05-01T00:00:00Z\$and:birth_date\$gte:01-06\$and:\$not:(age\$lt:25\$or:age\$gte:62)"
//        val filter = Filter.of(input, true)
//
//        val visitor = JPQLTransformationFilterVisitor(
//
//        filter.accept(visitor)
//
//        println(visitor.toWhereClause())
//    }

//    @Test
//    fun testEscaping() {
//        val input = "password\$like:\$\$\$*Noah*\$\$\$?\$\$?"
//        val filter = Filter.of(input, true)
//
//        val rootCondition = filter.rootCondition
//        val simpleConditionValue = ((rootCondition as Filter.Condition.SimpleCondition).value as Filter.Value.StringValue).value
//
//        println("Escaped simpleConditionValue: $simpleConditionValue")
//
//        assertEquals("\$**Noah*\$??\$?", simpleConditionValue)
//
//        val visitor = JPQLTransformationFilterVisitor()
//
//        filter.accept(visitor)
//
//        println(visitor.toWhereClause())
//
//        // Correct: $*Noah%$?$_
//
//        assertEquals("LOWER(password) LIKE LOWER('\$*Noah%\$?\$_')", visitor.toWhereClause())
//    }

   @ParameterizedTest
   @Tag("repository")
   @CsvSource(
      // Filtering by string with space
      "'','\$having:employees(first_name\$like:J*)', '', 'SELECT de_0 FROM Department de_0 WHERE de_0 IN (SELECT em_0.department FROM Employee em_0 WHERE LOWER(em_0.firstName) LIKE LOWER('J%'))'",
      "'','\$having:employees(\$having:projects(name\$eq:Apollo))', '', 'SELECT de_0 FROM Department de_0 WHERE de_0 IN (SELECT em_0.department FROM Employee em_0 WHERE em_0 IN (SELECT pr_0.members FROM Project pr_0 WHERE LOWER(pr_0.name) = LOWER('Apollo')))'",
      "'','\$having:COUNT(employees)\$gt:10', '', 'SELECT de_0 FROM Department de_0 WHERE de_0.id IN (SELECT em_0.department.id FROM Employee em_0 GROUP BY em_0.department.id HAVING COUNT(em_0.id) > 10)'",
      "'','name\$like:P*\$and:\$having:AVG(employees.year_of_birth)\$gte:2000', '', 'SELECT de_0 FROM Department de_0 WHERE (LOWER(de_0.name) LIKE LOWER('P%') AND de_0.id IN (SELECT em_0.department.id FROM Employee em_0 GROUP BY em_0.department.id HAVING AVG(em_0.yearOfBirth) >= 2000))'",
      "'','\$having:AVG(employees.year_of_birth)\$gte:2000', '', 'SELECT de_0 FROM Department de_0 WHERE de_0.id IN (SELECT em_0.department.id FROM Employee em_0 GROUP BY em_0.department.id HAVING AVG(em_0.yearOfBirth) >= 2000)'",

      )
   fun testHavingClause(paginationVal: String, filterVal: String, sortVal: String, expectedJpqlQuery: String) {
      // Select all departments that have employees with first name starting with J
      val filter = Filter.of(filterVal, true)

      val rootCondition = filter.rootCondition

      val visitor = JPQLTransformationFilterVisitor(
         metamodel = entityManager.metamodel,
         entityName = "Department",
         fieldTransformer = snakeToCamelTransformer
      )
      filter.accept(visitor)

      val query: String = visitor.toQuery()
      println("whereClause: ${visitor.toWhereClause()}")
      println("Query: $query")

      assertEquals(expectedJpqlQuery.trim('\''), query)
   }

   @Test
   @Tag("repository")
   fun testGetAllUsers() {
      val employees: List<Employee> = empRepo.findAll().toList()
      log.info("Users: {}", employees)
      assertEquals(20, employees.size)
   }

   @Test
   fun `Test employee query`() {
      val query: TypedQuery<Employee> = entityManager.createQuery(
         "SELECT e FROM Employee e WHERE lastName LIKE 'J%'",
         Employee::class.java
      )
      val employees: List<Employee> = query.resultList

      log.trace("Users:\n{}", employees.prettyPrint())

      // Assert that you received the correct number of users
//        assertEquals(20, users.size, "There should be exactly 20 users in the database.")
   }

   @Test
   fun `Test department query`() {
      val query: TypedQuery<Department> = entityManager.createQuery(
         """
                SELECT dep 
                FROM Department dep 
                WHERE (LOWER(dep.name) LIKE LOWER('%es') 
                  AND dep.id IN (
                          SELECT emp.department.id 
                          FROM Employee emp 
                          GROUP BY emp.department.id 
                          HAVING AVG(emp.yearOfBirth) >= 1950))
                """,
         Department::class.java
      )
      val departments: List<Department> = query.resultList

      log.trace("Departments:\n{}", departments.prettyPrint())
   }

   @ParameterizedTest
   @Tag("repository")
   @CsvSource(
      // Filtering by string with space
      "'\$page:1\$size:20','first_name\$eq:Michael$ *', '', 0",

      "'\$page:1\$size:20','first_name\$like:De$ la$ Vega', '', 0",

      "'\$page:1\$size:30','first_name\$eq:Emma', 'first_name', 1",

      // Filtering by first name and last name, expecting at least one result if there's a user with these exact names.
      "'\$page:1\$size:20','first_name\$eq:Noah\$and:last_name\$eq:Williams', '', 1",

      // Filtering users born before 1990, and sorting by birth year in descending order.
      "'\$page:1\$size:20','year_of_birth\$lt:1990', '-year_of_birth', 8",

      // Filtering by users living in 'USA', expecting multiple results, sorted by 'country' (though redundant since all are 'USA').
      "'\$page:1\$size:30','country\$eq:USA', 'first_name', 6",

      // Filtering users with a specific postal code and sorting by 'postal_code' in ascending order.
      "'\$page:1\$size:20','postal_code\$eq:10005', 'postal_code', 1",

      // Complex filtering: users with 'last_name' starting with 'A' and living in 'UK' or 'USA'.
      "'\$page:1\$size:20','last_name\$like:A*\$and:(country\$eq:UK\$or:country\$eq:USA)', '', 1",

      // Using 'IN' operator for numeric filtering, assuming these IDs exist.
      "'\$page:1\$size:30','id\$in:[1,2,3,4,5]', '', 5",

      // Filtering for users in a specific city and sorting by last name alphabetically.
      "'\$page:1\$size:30','postal_area\$eq:London', '~last_name', 2",

      // Filtering by a null value in 'address_line_2' and sorting by first name in descending order.
      "'\$page:1\$size:20','address_line_2\$null:', '-first_name', 14",

      // Filtering users born on a specific date.
      "'\$page:1\$size:20','birth_date\$eq:1990-06-23', '', 1",

      // Filtering users who last logged in after a specific datetime.
      "'\$page:1\$size:20','last_login\$gt:2023-06-01T00:00:00Z', 'last_login', 12",

      // Filtering users who last logged out before a specific datetime.
      "'\$page:1\$size:20','last_logout\$lt:2023-06-01T00:00:00Z', '-last_logout', 8",

      // Filtering users who have a non-null last login time.
      "'\$page:1\$size:30','last_login\$null:', 'last_login', 0",

      // Filtering users who have a non-null last logout time and sorting by last logout time in ascending order.
      "'\$page:1\$size:30','last_logout\$notnull:', 'last_logout', 20",

      // Filtering users by a date range of birth date.
      "'\$page:1\$size:20','birth_date\$gte:1980-01-01\$and:birth_date\$lte:1989-12-31', 'birth_date', 8",

      // Filtering users who last logged in within a specific date range.
      "'\$page:1\$size:30','last_login\$gte:2023-01-01T00:00:00Z\$and:last_login\$lte:2023-06-30T23:59:59Z', '', 10",

      // Filtering users who last logged out in May
      "'\$page:1\$size:20','last_logout\$gte:2023-05-01T00:00:00Z\$and:last_logout\$lte:2023-05-31T23:59:59Z', '', 2",

      "'\$page:1\$size:30','password\$eq:*Emma*', 'first_name', 1",

      // Testing literal asterisk and question mark '$*Noah$?$'
      "'\$page:1\$size:20','password\$like:\$\$\$*Noah\$\$\$?\$\$', '', 1",

      // Testing SQL wildcard pattern - DB value is 1li_er
//      "'\$page:1\$size:20','password\$like:1li_er', '', 1",

      // Testing month and day functions
      "'','birth_date\$gt:12-01', '', 2",

      // Testing time functions
      "'','last_login\$gt:09:00', '', 7",

      "'','last_logout\$gt:2022--', '-last_logout', 20",
      "'','first_name\$gt:John', '', 7",
      "'','first_name\$eq:John$(', '', 0",
      "'','is_part_time\$eq:TRUE', '', 1",
   )
   fun `test employee query with parameters`(paginationVal: String, filterVal: String, sortVal: String, expectedCount: Int) {
      val pagination = if (paginationVal.isNotEmpty()) Pagination.of(paginationVal) else Pagination()
      val filter = if (filterVal.isNotEmpty()) Filter.of(filterVal, true) else null
      val sorting = if (sortVal.isNotEmpty()) Sorting.of(sortVal) else null

      val page: Page<Employee> = empRepo.getEntities(
         pagination = pagination,
         filter = filter,
         sorting = sorting
      )

      // Assert
      assertEquals(expectedCount, page.numberOfElements, "Expected $expectedCount employees but found ${page.numberOfElements}")
   }

   @ParameterizedTest
   @Tag("repository")
   @CsvSource(
      "'', '\$having:COUNT(employees)\$gt:3', '', 4",
      "'', '\$having:employees(first_name\$like:J*)', '', 1",
   )
   fun `test department query with parameters`(paginationVal: String, filterVal: String, sortVal: String, expectedCount: Int) {
      val pagination = if (paginationVal.isNotEmpty()) Pagination.of(paginationVal) else Pagination()
      val filter = if (filterVal.isNotEmpty()) Filter.of(filterVal, true) else null
      val sorting = if (sortVal.isNotEmpty()) Sorting.of(sortVal) else null

      val page: Page<Department> = deptRepo.getEntities(
         pagination = pagination,
         filter = filter,
         sorting = sorting
      )

      // Assert
      assertEquals(
         expectedCount,
         page.numberOfElements,
         "Expected $expectedCount departments but found ${page.numberOfElements}"
      )
   }

}