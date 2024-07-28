package no.acntech.kollectiveq

import no.acntech.kollectiveq.util.text.EQUALS
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets

/**
 * Pagination class for building and parsing pagination strings for JPA or SQL queries. The pagination string format is
 * <code>"$page:[pageValue]$size:[sizeValue]</code> where <code>pageValue</code> and <code>sizeValue</code> are positive integers.
 * Pagination starts at 1.
 */
data class Pagination(
   val page: Int = DEFAULT_PAGE,
   val size: Int = DEFAULT_SIZE,
) {

   init {
      require(page > 0) { "Page '$page' must be greater than 0" }
      require(size > 0) { "Size '$size' must be greater than 0" }
   }

   val startIndex: Int
      get() = (page - 1) * size

   val endIndex: Int
      get() = startIndex + size

   val offset: Int
      get() = startIndex

   fun asString(): String {
      return "\$page:$page\$size:$size)"
   }

   fun toHttpParameter(): String {
      return "$QUERY_PARAM$EQUALS${UriUtils.encodeQueryParam(asString(), StandardCharsets.UTF_8)}"
   }

   companion object {

      const val DEFAULT_PAGE = 1
      const val DEFAULT_SIZE = 20
      const val PAGE_PARAM = "page"
      const val SIZE_PARAM = "size"
      const val QUERY_PARAM = "pagination"

      /**
       * Create a Pagination instance from a string.
       */
      fun of(value: String): Pagination {
         // Defaults
         var page = DEFAULT_PAGE
         var size = DEFAULT_SIZE

         // Parse the string
         value.let {
            val pattern = "\\$($PAGE_PARAM|$SIZE_PARAM):(\\d+)".toRegex()
            pattern.findAll(value).forEach { matchResult ->
               val (keyObject, valueObject) = matchResult.destructured
               when (keyObject) {
                  PAGE_PARAM -> page = valueObject.toInt()
                  SIZE_PARAM -> size = valueObject.toInt()
               }
            }
         }

         return Pagination(page, size)
      }
   }
}