package no.acntech.kollectiveq.time

/**
 * Parses a string into an [Instant] using a highly flexible formatter (parser) supporting a wide range of formats.
 *
 * This formatter supports a variety of formats, including times with or without seconds (and milliseconds). When no timezone
 * information is present, times are treated as UTC. Supported formats include:
 *
 * No offset (UTC by default):
 * - `2023-11-02T15:22`
 * - `2023-11-02T15:22:45`
 * - `2023-11-02T15:22:45.123`
 * - `2023-11-02T15:22:45.123456789`
 *
 * 'Z' offset (UTC):
 * - `2023-11-02T15:22Z`
 * - `2023-11-02T15:22:45Z`
 * - `2023-11-02T15:22:45.123Z`
 * - `2023-11-02T15:22:45.123456789Z`
 *
 * Timezone offset with colon (`+HH:MM` or `-HH:MM`):
 * - `2023-11-02T15:22+01:00`
 * - `2023-11-02T15:22:45+01:00`
 * - `2023-11-02T15:22:45-05:00`
 * - `2023-11-02T15:22:45.123+02:00`
 * - `2023-11-02T15:22:45.123456789-08:00`
 *
 * Timezone offset without colon (`+HHMM` or `-HHMM`):
 * - `2023-11-02T15:22+0100`
 * - `2023-11-02T15:22:45+0100`
 * - `2023-11-02T15:22:45-0500`
 * - `2023-11-02T15:22:45.123+0200`
 * - `2023-11-02T15:22:45.123456789-0800`
 *
 * Timezone offset with just hours (`+HH` or `-HH`):
 * - `2023-11-02T15:22+01`
 * - `2023-11-02T15:22:45+01`
 * - `2023-11-02T15:22:45-05`
 * - `2023-11-02T15:22:45.123+02`
 * - `2023-11-02T15:22:45.123456789-08`
 *
 * Full timezone ID:
 * - `2023-11-02T15:22[America/New_York]`
 * - `2023-11-02T15:22:45[America/New_York]`
 * - `2023-11-02T15:22:45.123[Europe/London]`
 * - `2023-11-02T15:22:45.123456789[Asia/Tokyo]`
 */


import java.time.*
import java.time.format.*
import java.time.temporal.ChronoField

object FlexibleInstantParser {

   private val FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
      // Date and Time
      .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm[:ss]]"))
      // Fraction of second
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
      .optionalEnd()
      // Time Zone or Offset (with colon, e.g., +HH:MM)
      .optionalStart()
      .appendOffset("+HH:MM", "Z")
      .optionalEnd()
      // Time Zone or Offset (without colon, e.g., +HHMM)
      .optionalStart()
      .appendOffset("+HHMM", "Z")
      .optionalEnd()
      // Time Zone or Offset (just hours, e.g., +HH)
      .optionalStart()
      .appendOffset("+HH", "Z")
      .optionalEnd()
      // Timezone ID (such as America/New_York)
      .optionalStart()
      .appendZoneRegionId()
      .optionalEnd()
      // Create the formatter
      .toFormatter()

   fun parse(input: String, defaultZoneId: ZoneId = ZoneId.systemDefault()): Instant {
      try {
         // First, try parsing as an Instant directly (for ISO_INSTANT and alike)
         return Instant.parse(input)
      } catch (ignored: DateTimeParseException) {

         // If that fails, try parsing with our custom formatter
         val temporalAccessor = FORMATTER.parseBest(
            input,
            ZonedDateTime::from,
            OffsetDateTime::from,
            LocalDateTime::from
         )

         return when (temporalAccessor) {
            is ZonedDateTime -> temporalAccessor.toInstant()
            is OffsetDateTime -> temporalAccessor.toInstant()
            is LocalDateTime -> temporalAccessor.atZone(defaultZoneId).toInstant()
            else -> throw DateTimeParseException("Failed to parse Instant from input string: $input", input, 0)
         }
      }
   }
}


