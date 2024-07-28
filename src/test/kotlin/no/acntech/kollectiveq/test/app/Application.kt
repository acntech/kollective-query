package no.acntech.kollectiveq.test.app

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.time.Instant

@SpringBootApplication
open class Application

private val log = LoggerFactory.getLogger(Application::class.java)

fun main(args: Array<String>) {
   val app = SpringApplication(Application::class.java)
   app.setAdditionalProfiles("default")
   app.run(*args)
}

@PreDestroy
private fun destroy() {
   log.warn("@PreDestroy callback: Application terminating at ${Instant.now()}")
}
