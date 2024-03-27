# KollectiveQuery - yet another QueryDSL

## Table of Contents

1. [Introduction](#Introduction)
2. [Getting Started](#Getting-Started)
3. [Requirements](#Requirements)
4. [Configuration](#Configuration)
5. [Examples](#Examples)
6. [API Reference](#API-Reference)
7. [Known Issues](#Known-Issues)
8. [Troubleshooting](#Troubleshooting)
9. [FAQs](#FAQs)
10. [Community and Support](#Community-and-Support)
11. [Acknowledgments](#Acknowledgments)
12. [Roadmap](#Roadmap)
13. [Contributing](#Contributing)
14. [License](#License)

## Introduction

KollectiveQuery is a DSL for filtering, sorting and paginating collections, primarily used by front-end apps over HTTP/REST. The DSL can typically be used on REST collection endpoints to build dynamic queries. The DSL is inspired by the [mongoDB query language](https://docs.mongodb.com/manual/reference/operator/query/).

## Getting Started

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
- Thomas Muller (thomas.muller@accenture.com): main contributor and maintainer

## License
This software is licensed under the Apache 2 license, see LICENSE and NOTICE for details.