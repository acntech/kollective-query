package no.acntech.kollectiveq.test.app.util.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import no.acntech.kollectiveq.test.app.util.time.TimeFormatUtils
import java.io.IOException
import java.time.Instant

class Fraction0InstantSerializer : JsonSerializer<Instant>() {

   @Throws(IOException::class)
   override fun serialize(
      value: Instant,
      gen: JsonGenerator,
      serializers: SerializerProvider,
   ) {
      gen.writeString(TimeFormatUtils.ISO_INSTANT_0_FRACTIONAL_DIGITS.format(value))
   }

}
