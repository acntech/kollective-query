package no.acntech.kollectiveq.test.app.util.time

import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import java.util.concurrent.TimeUnit

object TimeFormatUtils {

   const val SECONDS_AND_MILLIS_DURATION_FORMAT = "ss.SSS"

   val SIMPLE_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

   val ISO_INSTANT_3_FRACTIONAL_DIGITS: DateTimeFormatter = DateTimeFormatterBuilder().appendInstant(3).toFormatter()

   val ISO_INSTANT_0_FRACTIONAL_DIGITS: DateTimeFormatter = DateTimeFormatterBuilder().appendInstant(0).toFormatter()

   fun formatDurationAsSecondsAndMillis(duration: Duration): String {
      return formatDurationAsSecondsAndMillis(duration.toMillis())
   }

   fun formatDurationAsSecondsAndMillis(time: Long, unit: TimeUnit): String {
      return DurationFormatUtils.formatDuration(unit.toMillis(time), SECONDS_AND_MILLIS_DURATION_FORMAT)
   }

   fun formatDurationAsSecondsAndMillis(millis: Long): String {
      return DurationFormatUtils.formatDuration(millis, SECONDS_AND_MILLIS_DURATION_FORMAT)
   }

   fun format(instant: Instant, tz: TimeZone, pattern: String): String {
      return format(instant, tz.toZoneId(), pattern)
   }

   fun format(instant: Instant, zoneId: ZoneId, pattern: String): String {
      return DateTimeFormatter.ofPattern(pattern).format(instant.atZone(zoneId))
   }

   fun format(instant: Instant, timeZone: TimeZone, dateTimeFormatter: DateTimeFormatter): String {
      return dateTimeFormatter.format(instant.atZone(timeZone.toZoneId()))
   }

   fun format(instant: Instant, zoneId: ZoneId, dateTimeFormatter: DateTimeFormatter): String {
      return dateTimeFormatter.format(instant.atZone(zoneId))
   }

   /**
    * Returns the given instant formatted as 'yyyy-MM-dd HH:mm:ss'.
    */
   fun formatSimpleLocalDateTime(instant: Instant, timeZone: TimeZone): String {
      return formatSimpleLocalDateTime(instant, timeZone.toZoneId())
   }

   /**
    * Returns the given instant formatted as 'yyyy-MM-dd HH:mm:ss'.
    */
   fun formatSimpleLocalDateTime(instant: Instant, zoneId: ZoneId): String {
      return SIMPLE_DATE_TIME_FORMATTER.format(instant.atZone(zoneId))
   }
}
