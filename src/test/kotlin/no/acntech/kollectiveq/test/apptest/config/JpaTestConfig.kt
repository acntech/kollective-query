package no.acntech.app.config

import no.acntech.kollectiveq.test.app.config.JpaConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(JpaConfig::class)
open class JpaTestConfig
