package no.acntech.kollectiveq.test.app.util.json

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
//import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.Instant

object JsonUtils {

   val LENIENT_MAPPER: ObjectMapper =
      Jackson2ObjectMapperBuilder()
         .serializationInclusion(JsonInclude.Include.NON_NULL)
         .featuresToEnable(
            SerializationFeature.INDENT_OUTPUT,
            MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES,
            MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS
         )
         .featuresToDisable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            SerializationFeature.FAIL_ON_EMPTY_BEANS
         )
         .serializers()
         .build<ObjectMapper>()
         .findAndRegisterModules()
//            .registerKotlinModule()
         .registerModule(
            SimpleModule()
               .addSerializer(Instant::class.java, Fraction3InstantSerializer())
               .addDeserializer(Instant::class.java, FlexibleInstantDeserializer())
         )
         .setPropertyNamingStrategy(PropertyNamingStrategies.SnakeCaseStrategy())
         .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
//            .configure(MapperFeature.AUTO_DETECT_GETTERS, true)
         .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

   fun prettyPrintWithIndent(node: Any, baseIndent: String, objectMapper: ObjectMapper = LENIENT_MAPPER): String {
      // Get the pretty printed string
      val prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)

      // Add the baseIndent to the start of every line
      return prettyJson.lines().joinToString("\n") { "$baseIndent$it" }
   }

}