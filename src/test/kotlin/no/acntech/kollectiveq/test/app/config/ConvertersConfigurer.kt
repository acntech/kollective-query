package no.acntech.kollectiveq.test.app.config

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
