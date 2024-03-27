package no.acntech.kollectiveq.test

import no.acntech.kollectiveq.Sorting
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SortingTests {


   @ParameterizedTest
   @CsvSource(
      "'firstName,lastName', 2, 'firstName', 'ASC', 'lastName', 'ASC'",
      "'-firstName,~lastName', 2, 'firstName', 'DESC', 'lastName', 'ASC_ALPH'",
      "'~firstName,-lastName', 2, 'firstName', 'ASC_ALPH', 'lastName', 'DESC'",
      "'~-firstName', 1, 'firstName', 'DESC_ALPH', '', ''", // Provide empty strings for the second criterion
      "'lastName', 1, 'lastName', 'ASC', '', ''", // Provide empty strings for the second criterion
      delimiter = ','
   )
   fun `test sorting parsing`(
      input: String,
      expectedSize: Int,
      firstField: String,
      firstDirection: String,
      secondField: String?,
      secondDirection: String?,
   ) {
      val sorting = Sorting.of(input)

      assertEquals(expectedSize, sorting.criteria.size, "Number of criteria parsed does not match expected")

      // Verify first criterion
      assertNotNull(sorting.criteria.firstOrNull(), "First criterion should not be null")
      assertEquals(firstField, sorting.criteria[0].field, "First field does not match expected")
      assertEquals(
         Sorting.Direction.valueOf(firstDirection),
         sorting.criteria[0].direction,
         "First direction does not match expected"
      )

      // Verify second criterion if provided
      if (!secondField.isNullOrEmpty()) {
         assertNotNull(sorting.criteria.getOrNull(1), "Second criterion should not be null")
         assertEquals(secondField, sorting.criteria[1].field, "Second field does not match expected")
         assertEquals(
            Sorting.Direction.valueOf(secondDirection!!),
            sorting.criteria[1].direction,
            "Second direction does not match expected"
         )
      }
   }

}