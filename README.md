# KollectiveQuery - yet another QueryDSL

## Table of Contents

1. [Introduction](#Introduction)
2. [Getting Started](#Getting-Started)
3. [The Query DSL](#The-Query-DSL)   
   1. [Pagination](#Pagination)
   2. [Filtering](#Filtering)
      1. [URL Encoding](#URL-Encoding)
      2. [Specifying Operator affinity](#Specifying-Operator-affinity)
      3. [Filter Operators](#Filter-Operators)
      4. [Sub-queries](#Sub-queries)
      5. [Substring matching](#Substring-matching)
      6. [Escaping special characters](#Escaping-special-characters)
      7. [Null values](#Null-values)
      8. [Timestamps and time related values](#Timestamps-and-time-related-values)
      9. [Using in and not in operators](#Using-in-and-not-in-operators) 
4. [Requirements](#Requirements)
5. [Configuration](#Configuration)
6. [Security](#Security)
7. [Examples](#Examples)
8. [API Reference](#API-Reference)
9. [Known Issues](#Known-Issues)
10. [Troubleshooting](#Troubleshooting)
11. [FAQs](#FAQs)
12. [Community and Support](#Community-and-Support)
13. [Acknowledgments](#Acknowledgments)
14. [Roadmap](#Roadmap)
15. [Contributing](#Contributing)
16. [License](#License)

## Introduction
KollectiveQuery introduces a streamlined DSL (Domain-Specific Language) for effortlessly filtering, sorting, and paginating data, making it the ideal tool for developers crafting dynamic queries for RESTful services. Drawing inspiration from the expressive power of the MongoDB query language, KollectiveQuery elevates querying capabilities within HTTP/REST applications.

Our DSL abstracts queries into a storage-agnostic structure, which is then seamlessly translated into the specific syntax of target query languages. Currently, we support JPQL with plans to extend our support to SQL and beyond, broadening our adaptability and utility.

- **Pagination**: Define both the page and results per page in a compact format, enabling efficient data navigation.
Example: `pagination=$page:4$size:20`

- **Sorting**: Easily arrange your data by specifying fields and directions using straightforward symbols.
Example: `sort=-first_name,last_name,~-age` - sorts by first name (descending), then by last name (ascending), and finally by age (alphabetically descending).

- **Filtering**: The core of KollectiveQuery - a robust system supporting logical operators and a variety of conditions to refine your searches to the finest detail.

Example 1: To find resources with first_name starting with 'J', last_name ending with 'son', last login before May 2023, a birthdate on or after June 1st, and an age between 25 and 62:
  `filter=first_name$like:J*$and:last_name$like:*son$and:last_login$lt:2023-05-01T00:00:00Z$and:birth_date$gte:06-01$and:$not:(age$lt:25$or:age$gte:62)`

Example 2: Get all departments with at least one employee having first name Joe born before 1990:
  `filter=$having:employees(first_name$eq:Joe$and:birth_date$lt:1990)`

Example 3: Get all departments with a name starting with the letter 'P' having employees with an average birth-year greater than or equal to 2000: `name$like:P*$and:$having:AVG(employees.year_of_birth)$gte:2000`

The elegance of KollectiveQuery is not just in its DSL but in its ability to transform complex filters into an optimized query, tailored for JPQL or any future supported query language. This transformative capability ensures that KollectiveQuery remains a versatile, powerful tool for developers aiming to enhance the interactivity and responsiveness of their applications.

## Getting Started

To add filter mechanics to JPA repositories, you need to follow these steps:

1. Add a Spring Configuration class to configure `EntityScan` and `EnableJpaRepositories`:

Kotlin
```kotlin
import no.acntech.kollectiveq.persistence.DefaultFilterRepository
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(basePackages = ["package.to.scan.for.entities"])
@EnableJpaRepositories(
   basePackages = [
      "no.acntech.kollectiveq.persistence",
      "package.to.scan.for.repositories"
   ],
   repositoryBaseClass = DefaultFilterRepository::class
)
open class JpaConfig
```

Java
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import no.acntech.kollectiveq.persistence.DefaultFilterRepository;

@Configuration
@EntityScan(basePackages = {"package.to.scan.for.entities"})
@EnableJpaRepositories(
    basePackages = {
        // The package where the FilterRepository and its default implementation is located
        "no.acntech.kollectiveq.persistence",
        // The package where the application JPA repositories are located
        "package.to.scan.for.repositories"
    },
    repositoryBaseClass = DefaultFilterRepository.class
)
public class JpaConfig {
  // Any additional configuration or beans can go here
}
```

2. Enable filtering (and pagination and sorting) by using [`FilterRepository`](https://github.com/acntech/kollective-query/blob/main/src/main/kotlin/no/acntech/kollectiveq/persistence/FilterRepository.kt) as a super-interface for your JPA repositories: 

Kotlin
```kotlin
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
```

Java
```java
import com.google.common.base.CaseFormat;
import no.acntech.kollectiveq.Filter;
import no.acntech.kollectiveq.Pagination;
import no.acntech.kollectiveq.Sorting;
import no.acntech.kollectiveq.lang.TransformFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.io.Serializable;
import java.util.function.Function;

@NoRepositoryBean
public interface FilterRepository<E, ID extends Serializable> extends JpaRepository<E, ID> {

    default Function<String, String> createCaseFormatTransformFunction(CaseFormat fromFormat, CaseFormat toFormat) {
        return input -> fromFormat.to(toFormat, input);
    }

    Page<E> getEntities(Pagination pagination, Filter filter, Sorting sorting, Function<String, String> fieldTransformer);
}
```

3. Add converters to allow HTTP query params to be converted to [`Filter`](https://github.com/acntech/kollective-query/blob/main/src/main/kotlin/no/acntech/kollectiveq/Filter.kt), [`Pagination`](https://github.com/acntech/kollective-query/blob/main/src/main/kotlin/no/acntech/kollectiveq/Pagination.kt) and [`Sorting`](https://github.com/acntech/kollective-query/blob/main/src/main/kotlin/no/acntech/kollectiveq/Sorting.kt) objects:

Kotlin

```kotlin
import no.acntech.kollectiveq.Filter
import no.acntech.kollectiveq.Pagination
import no.acntech.kollectiveq.Sorting
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class ConvertersConfigurer(
   private val filterConverter: Converter<String, Filter>,
   private val sortingConverter: Converter<String, Sorting>,
   private val paginationConverter: Converter<String, Pagination>,
) : WebMvcConfigurer {

   @Component
   class FilterConverter : Converter<String, Filter> {
      override fun convert(source: String): Filter {
         return Filter.of(source)
      }
   }

   @Component
   class SortingConverter : Converter<String, Sorting> {
      override fun convert(source: String): Sorting {
         return Sorting.of(source)
      }
   }

   @Component
   class PaginationConverter : Converter<String, Pagination> {
      override fun convert(source: String): Pagination {
         return Pagination.of(source)
      }
   }

   override fun addFormatters(registry: FormatterRegistry) {
      registry.addConverter(filterConverter)
      registry.addConverter(sortingConverter)
      registry.addConverter(paginationConverter)
   }

}
```

Java

```java
import no.acntech.kollectiveq.Filter;
import no.acntech.kollectiveq.Pagination;
import no.acntech.kollectiveq.Sorting;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConvertersConfigurer implements WebMvcConfigurer {

    private final Converter<String, Filter> filterConverter;
    private final Converter<String, Sorting> sortingConverter;
    private final Converter<String, Pagination> paginationConverter;

    // Constructor injection in Java
    public ConvertersConfigurer(Converter<String, Filter> filterConverter,
                                 Converter<String, Sorting> sortingConverter,
                                 Converter<String, Pagination> paginationConverter) {
        this.filterConverter = filterConverter;
        this.sortingConverter = sortingConverter;
        this.paginationConverter = paginationConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(filterConverter);
        registry.addConverter(sortingConverter);
        registry.addConverter(paginationConverter);
    }

    // Inner classes for converters
    @Component
    public static class FilterConverter implements Converter<String, Filter> {
        @Override
        public Filter convert(String source) {
            return Filter.of(source);
        }
    }

    @Component
    public static class SortingConverter implements Converter<String, Sorting> {
        @Override
        public Sorting convert(String source) {
            return Sorting.of(source);
        }
    }

    @Component
    public static class PaginationConverter implements Converter<String, Pagination> {
        @Override
        public Pagination convert(String source) {
            return Pagination.of(source);
        }
    }
}
```

4. Use the repositories from a `@Controller`, `@RestController` or `@Service`:

Kotlin

```kotlin
@RestController
@RequestMapping("/employees")
class EmployeeController(
   private val employeeRepo: EmployeeRepository,
) {
   @GetMapping
   fun getAll(
      @RequestParam(name = "pagination", required = false) pagination: Pagination?,
      @RequestParam(name = "filter", required = false) filter: Filter?,
      @RequestParam(name = "sort", required = false) sorting: Sorting?,
   ): ResponseEntity<List<Employee>> {
      val effectivePagination = pagination ?: Pagination()
      val employees = employeeRepo.getEntities(effectivePagination, filter, sorting)
      return ResponseEntity.ok(employees.content)
   }

}
```

Java

```java
@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepo;

    @Autowired
    public EmployeeController(EmployeeRepository employeeRepo) {
        this.employeeRepo = employeeRepo;
    }

    @GetMapping
    public ResponseEntity<List<Employee>> getAll(
            @RequestParam(name = "pagination", required = false) Pagination pagination,
            @RequestParam(name = "filter", required = false) Filter filter,
            @RequestParam(name = "sort", required = false) Sorting sorting) {
        // In Java, you need to handle the null case explicitly, as Java does not have a safe call operator like Kotlin.
        Pagination effectivePagination = pagination != null ? pagination : new Pagination();
        Page<Employee> employees = employeeRepo.getEntities(effectivePagination, filter, sorting);
        return ResponseEntity.ok(employees.getContent());
    }
}
```

## The Query DSL

### Pagination
Pagination on collections can be done using the query string parameter `pagination`. The pagination parameter is made up of a set of key-value pairs, separated by the dollar sign ($). The key is the name of the pagination parameter, and the value is the value of the pagination parameter.

Example: `pagination=$page:1$size:20` - Returns the first 20 resources.

Example: `pagination=$page:2$size:20` - Returns the next 20 resources.

### Filtering
Filtering on collections can be done using the query string parameter `filter`. A filter is made up of a set of predicates, and follows a syntax inspired by mongoDB. A predicate is made up of a property name, an operator and a value.

Example: `filter=name$eq:Joe` - Matches all persons with the value Joe in the field 'name'.

This matches all resources with the value Joe in the property 'name'.

Predicates can be chained using either of the logical operators AND and OR. NOT is supported to negate a predicate.

Example: `filter=name$eq:Joe$and:city$like:*port$and:country$ne:Norway` - Matches all persons with the value Joe in the field 'name' and where the city contains the string 'port' and where the country is not Norway.

Note that filtering on strings is *case-insensitive*.

Example: `filter=age$gt:40` - Matches all persons whose age is greater than 40.

Example: `filter=is_married$eq:TRUE` - Matches all persons who are married.

#### URL Encoding
URL parameter values should always be URL compatible. Always URL encode filter strings.

#### Specifying Operator affinity
Parentheses are used to control operator affinity.

Example:`filter=name$eq:Joe$and:$not:(city$like:*port$or:age$lt:40)` - Matches all persons with the value Joe in the field 'name' and where the city does not contain the string 'port' or the age is less than 40.

#### Filter Operators
The allowed filtering operators are:

| Operator              | Syntax     |
|-----------------------|------------|
| Equals                | `$eq:`     |
| Not equals            | `$ne:`     |
| Greater than          | `$gt:`     |
| Greater than or equal | `$gte:`    |
| Less than             | `$lt:`     |
| Less than or equal    | `$lte:`    |
| Is NULL               | `$null:`   |
| Is NOT NULL           | `$nnull:`  |
| Substring match       | `$like:`   |
| And also              | `$and:`    |
| Or else               | `$or:`     |
| Not                   | `$not:`    |
| In                    | `$in:`     |
| Not In                | `$nin:`    |
| Having                | `$having:` |

#### Sub-queries
The `$having:` operator allows you to specify a sub-query for criteria purposes. The sub-query is specified as a filter expression using the following syntax: `$having:<field-name>(<filter expression>)`.

The sub-query is evaluated for each resource in the collection and only matching resources are returned.

Example: Assume querying the departments endpoint, and the there is a one-to-many relationship between departments and employees. The following query will return all departments that have at least one employee with the name 'Joe': `filter=$having:employees(name$eq:Joe)` 

The `$having:` can also be followed by a FUNCTION on a related entity. The following functions are supported: `count`, `sum`, `avg`, `min`, `max`.

Example: Assume querying the departments endpoint, and the there is a one-to-many relationship between departments and employees. The following query will return all departments having more than 10 employees: `filter=$having:count(employees)$gt:10`

Assume Employee has a property 'age' with an integer representing the age of the employee. The following query will return all departments having an average employee age of more than 40: `filter=$having:avg(employees.age)$gt:40`

The `$having:` clause can be used in combination with other operators, like `$and:` and `$or:` and `$not:`. 

PLEASE NOTE: Nested `$having:` clauses are not supported.


#### Substring matching
The `$like:` filter supports both using wildcards (*) (multiple characters), and (?) for a single wildcard - and not using wildcards. If no wildcards are used, the expression is considered a *contains* expression and effectively becomes a filter with a wildcard at the start of the string and one at the end of the string.

Example: `first_name$like:A*$and:last_name$like:J*son` - Matches all persons with first name starting with 'A' and last name starting with a 'J' and ending with 'son' (like 'Johnson' or 'Jackson').

#### Escaping special characters
In order to not interfere with the parsing of the filter expression, certain escape sequences are necessary. The dollar sign ($) is used as an escape character. The following characters are escaped:


| Symbol | Escaped |
|--------|---------|
| `$`    | `$$`    |
| `(`    | `$(`    |
| `)`    | `$)`    |
| `*`    | `$*`    |
| `?`    | `$?`    |
| `,`    | `$,`    |
| `[`    | `$[`    |
| `]`    | `$]`    |
| `:`    | `$:`    |
| `-`    | `$-`    |
| ` `    | `$ `    |

#### Null values
Should you want to filter for the non-existence of a property (i.e. null value) you can use the null escape sequence.

`$null:`

Should you want to filter for the existence of a property (i.e. NOT null value) you can use the not null escape sequence: `$nnull:`

#### Timestamps and time related values
For absolute times the following formats are supported:

| Format                                     | Example                                       |
|--------------------------------------------| --------------------------------------------- |
| `yyyy-MM-dd'T'HH:mm`                       | `2023-11-02T15:22`                            |
| `yyyy-MM-dd'T'HH:mm:ss`                    | `2023-11-02T15:22:45`                         |
| `yyyy-MM-dd'T'HH:mm:ss.SSS`                | `2023-11-02T15:22:45.123`                     |
| `yyyy-MM-dd'T'HH:mm:ss.SSSZ`               | `2023-11-02T15:22:45.123Z`                    |
| `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`             | `2023-11-02T15:22:45.123+01:00`               |
| `yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS`          | `2023-11-02T15:22:45.123456789`               |
| `yyyy-MM-dd'T'HH:mmZ`                      | `2023-11-02T15:22Z`                           |
| `yyyy-MM-dd'T'HH:mm:ssZ`                   | `2023-11-02T15:22:45Z`                        |
| `yyyy-MM-dd'T'HH:mm:ss.SSSZ`               | `2023-11-02T15:22:45.123Z` (duplicate)        |
| `yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ`         | `2023-11-02T15:22:45.123456789Z`              |
| `yyyy-MM-dd'T'HH:mm+HH:MM`                 | `2023-11-02T15:22+01:00`                      |
| `yyyy-MM-dd'T'HH:mm:ss-SS:SS`              | `2023-11-02T15:22:45-05:00`                   |
| `yyyy-MM-dd'T'HH:mm:ss.SSS+HHMM`           | `2023-11-02T15:22:45.123+0200`                |
| `yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS-SSSS`     | `2023-11-02T15:22:45.123456789-0800`        |
| `yyyy-MM-dd'T'HH:mm+HH`                    | `2023-11-02T15:22+01`                         |
| `yyyy-MM-dd'T'HH:mm:ss-SS`                 | `2023-11-02T15:22:45-05`                      |
| `yyyy-MM-dd'T'HH::mm:ss.SSS+HH`            | `2023-11-02T15:22:45.123+02`                  |
| `yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS-SS`       | `2023-11-02T15:22:45.123456789-08`            |
| `yyyy-MM-dd'T'HH:mm[ZoneID]`               | `2023-11-02T15:22[America/New_York]`  |
| `yyyy-MM-dd'T'HH:mm:ss[ZoneID]`            | `2023-11-02T15:22:45[America/New_York]`      |
| `yyyy-MM-dd'T'HH:mm:ss.SSS[ZoneID]`        | `2023-11-02T15:22:45.123[Europe/London]`      |
| `yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS[ZoneID]`  | `2023-11-02T15:22:45.123456789[Asia/Tokyo]` |
| `yyyy-MM-dd'T'HH:mm[ZoneID]`               | `2023-11-02T15:22[Europe/London]`             |

The DSL also supports extracting specific time fields for comparison:

Example: `filter=birth_date$eq:01-25` - Matches all persons with birthdate on the 25th of January (irrespective of year).

Example: `filter=birth_date$gte:1995--` - Matches all persons born in or after 1995

Example: `filter=last_login$gte:12:15:00` - Matches all persons who last logged in after 12:15:00 (irrespective of date).

#### Using in and not in operators
To determine whether a specified value matches any value in (or not in) a list you filter using the `$in:` or `$nin:` operator. The list to filter by has to be enclosed in brackets and values seperated by commas.

`customer_number$in:[2,5,7,22,45]`

Numeric, string and date/timestamp attributes are allowed in the list. NULL `$null:` and `$nnull:` are not supported in the list.

### Sorting
Sorting on collections can be done using the query string parameter `sort`. Note that sorting on strings is *case-insensitive*.

#### Sort ascending
Example: `sort=name`

#### Sort descending
The default sort direction is ascending, but this can be turned by prepending a minus (-).

`sort=-name` - Sort by name descending.

#### Sort by multiple properties
If you need to sort by multiple properties these can just be separated by commas. Mixing of directions is allowed.

Example: `sort=-name,age` - Sort by name descending, then age ascending.

#### Sort alphabetically
In certain cases you might want to enforce that even numeric values are sorted alphabetically, so e.g. 1000 is less than 30. In those cases you can prepend the sort property with a tilde (~).

Example: `sort=~age` - Sort by age alphabetically.

To sort numeric data in descending alphabetic order, the minus operator is allowed after the tilde, like this:

Example: `sort=~-age` - Sort by age descending alphabetically.

## Requirements
TODO

## Configuration
No specific configuration is needed to use the library. However, to use the library with JPA repositories, you need to configure the [`FilterRepository`](https://github.com/acntech/kollective-query/blob/main/src/main/kotlin/no/acntech/kollectiveq/persistence/FilterRepository.kt) as a super-interface for your JPA repositories and make sure to add converters to allow HTTP query params to be converted to the Filter, Sorting and Pagination objects.

## Security
Dynamic query generation is inherently vulnerable to SQL/query language injection attacks. Always traverse the parsed [Filter](https://github.com/acntech/kollective-query/blob/main/src/main/kotlin/no/acntech/kollectiveq/Filter.kt), [Sorting](https://github.com/acntech/kollective-query/blob/main/src/main/kotlin/no/acntech/kollectiveq/Sorting.kt) and [Pagination](https://github.com/acntech/kollective-query/blob/main/src/main/kotlin/no/acntech/kollectiveq/Pagination.kt) structures with validators before or during transformation to the specific query language (note: only JPQL transformation is supported in the current version). E.g. for SQL and JPQL - never allow sorting on non-indexed attributes, limit the fields that can be used for filtering, limit the depth of and/or/not constructions, limit the length of the filter etc. Consider the risk and relevant mitigation techniques for your particular project before executing dynamically generated queries.

## Examples
* See the Spring Boot application [test case](https://github.com/acntech/kollective-query/tree/main/src/test/kotlin/no/acntech/kollectiveq/test/app)
    * Configuration: [JPA Config](https://github.com/acntech/kollective-query/blob/main/src/test/kotlin/no/acntech/kollectiveq/test/app/config/JpaConfig.kt) [Converters](https://github.com/acntech/kollective-query/blob/main/src/test/kotlin/no/acntech/kollectiveq/test/app/config/ConvertersConfigurer.kt)
    * Repository: [EmployeeRepository](https://github.com/acntech/kollective-query/blob/main/src/test/kotlin/no/acntech/kollectiveq/test/app/domain/repository/EmployeeRepository.kt)
    * Controller: [EmployeeController](https://github.com/acntech/kollective-query/blob/main/src/test/kotlin/no/acntech/kollectiveq/test/app/controller/EmployeeController.kt)
    * Service: [EmployeeService](https://github.com/acntech/kollective-query/blob/main/src/test/kotlin/no/acntech/kollectiveq/test/app/domain/core/DefaultEmployeeService.kt)
    * [Repo tests](https://github.com/acntech/kollective-query/blob/main/src/test/kotlin/no/acntech/kollectiveq/test/apptest/domain/repostitory/MiscRepoTests.kt)
* See [FilterTest.kt](https://github.com/acntech/kollective-query/blob/main/src/test/kotlin/no/acntech/kollectiveq/test/FilterTest.kt)
* See [SortingTests.kt](https://github.com/acntech/kollective-query/blob/main/src/test/kotlin/no/acntech/kollectiveq/test/SortingTests.kt)

## API Reference
- [JavaDoc](http://blog.acntech.no/kollective-query/apidocs/javadoc/index.html)
- [KDoc](http://blog.acntech.no/kollective-query/apidocs/kdoc/index.html)

## Known Issues
- Deeply nested structures will (most likely) fail to parse and transform correctly. Test thoroughly before using deeply nested structures.

## Troubleshooting
TODO

## FAQs
TODO

## Community and Support
For issues and bugs please submit an issue on the [GitHub repository](https://github.com/acntech/kollective-query/issues). Also feel free to contact the main contributor and maintainer directly at his [personal email](mailto:me.thomas.muller@gmail.com) or [work email](mailto:thomas.muller@accenture.com).

## Acknowledgments
TODO

## Roadmap
- [ ] Add support for SQL transformation
- [ ] Add support for MongoDB query language transformation
- [ ] Add support for ElasticSearch query language transformation
- [ ] Add support for more complex queries and sub-queries 

## Contributing
- [Thomas Muller](mailto:thomas.muller@accenture.com) ([personal email](mailto:me.thomas.muller@gmail.com)): main contributor and maintainer

## License
This software is licensed under the Apache 2 license, see [LICENSE](https://github.com/acntech/kollective-query/blob/main/LICENSE) and [NOTICE](https://github.com/acntech/kollective-query/blob/main/NOTICE) for details.