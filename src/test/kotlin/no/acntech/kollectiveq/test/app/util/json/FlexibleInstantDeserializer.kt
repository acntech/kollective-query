package no.acntech.kollectiveq.test.app.util.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import no.acntech.kollectiveq.util.time.FlexibleInstantParser
import java.time.Instant
import java.time.format.DateTimeParseException

class FlexibleInstantDeserializer : JsonDeserializer<Instant>() {

   override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant {
      val dateString = p.text
      try {
         return FlexibleInstantParser.parse(dateString)
      } catch (e: DateTimeParseException) {
         throw ctxt.weirdStringException(dateString, Instant::class.java, e.message)
      }
   }

}

