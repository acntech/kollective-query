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
6. [Examples](#Examples)
7. [API Reference](#API-Reference)
8. [Known Issues](#Known-Issues)
9. [Troubleshooting](#Troubleshooting)
10. [FAQs](#FAQs)
11. [Community and Support](#Community-and-Support)
12. [Acknowledgments](#Acknowledgments)
13. [Roadmap](#Roadmap)
14. [Contributing](#Contributing)
15. [License](#License)

## Introduction

KollectiveQuery is a DSL for filtering, sorting and paginating collections, primarily used by front-end apps over HTTP/REST. The DSL can typically be used on REST collection endpoints to build dynamic queries. The DSL is inspired by the [mongoDB query language](https://docs.mongodb.com/manual/reference/operator/query/).

## Getting Started

To add filter mechanics to JPA repositories, you need to
1. Add a Spring Configuration class to configure EntityScan and EnableJpaRepositories

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
        "no.acntech.kollectiveq.persistence",
        "package.to.scan.for.repositories"
    },
    repositoryBaseClass = DefaultFilterRepository.class
)
public class JpaConfig {
  // Any additional configuration or beans can go here
}
```

2. Enable filtering (and pagination and sorting) by using `FilterRepository` as a super-interface for your JPA repositories. 

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

3. Add converters to allow HTTP query params to be converted to `Filter`, `Pagination` and `Sorting` objects.

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

4. Use the repositories from a Controller or a Service

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

The sub-query is evaluated for each resour[MiscRepoTests.kt](src%2Ftest%2Fkotlin%2Fno%2Facntech%2Fapp%2Fdomain%2Frepostitory%2FMiscRepoTests.kt)ce in the collection and only matching resources are returned.

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
In certain cases you might want to enforce that even numeric values are sorted alphabetically, so 1000 is less than 30. In those cases you can prepend the sort property with a tilde (~).

Example: `sort=~age` - Sort by age alphabetically.

To sort numeric data in descending alphabetic order, the minus operator is allowed after the tilde, like this:

Example: `sort=~-age` - Sort by age descending alphabetically.

## Requirements
TODO

## Configuration
TODO

## Examples
TODO

## API Reference
TODO

## Known Issues
TODO

## Troubleshooting
TODO

## FAQs
TODO

## Community and Support
TODO

## Acknowledgments
TODO

## Roadmap
TODO

## Contributing
- Thomas Muller (thomas.muller@accenture.com) (me.thomas.muller@gmail.com): main contributor and maintainer

## License
This software is licensed under the Apache 2 license, see LICENSE and NOTICE for details.