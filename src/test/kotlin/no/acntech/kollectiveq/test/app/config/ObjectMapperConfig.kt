package no.acntech.kollectiveq.test.app.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.acntech.kollectiveq.test.app.util.json.JsonUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
open class ObjectMapperConfig {

   @Bean
   @Primary
   open fun objectMapper(): ObjectMapper {
      return JsonUtils.LENIENT_MAPPER
   }

}
