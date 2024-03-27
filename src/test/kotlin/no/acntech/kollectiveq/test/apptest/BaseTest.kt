package no.acntech.kollectiveq.test.apptest

import no.acntech.kollectiveq.test.app.util.json.JsonUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource

abstract class BaseTest {

   protected val log: Logger = LoggerFactory.getLogger(javaClass)

   protected val objectMapper = JsonUtils.LENIENT_MAPPER

   @Autowired
   private lateinit var dataSource: DataSource

}