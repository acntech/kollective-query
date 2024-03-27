package no.acntech.kollectiveq.text

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.text.ParseException

class DetailedParseException : ParseException {

   var lineNumber: Int = -1
      private set

   var columnNumber: Int = -1
      private set

   // Primary constructor
   constructor() : super(null, -1)

   // Secondary constructors
   constructor(msg: String?) : super(msg, -1)

   constructor(cause: Throwable?) : super(cause?.message, -1) {
      initCause(cause)
   }

   constructor(msg: String?, cause: Throwable?) : super(msg, -1) {
      initCause(cause)
   }

   constructor(msg: String?, cause: Throwable?, errorOffset: Int) : super(msg, errorOffset) {
      initCause(cause)
   }

   constructor(msg: String?, cause: Throwable?, lineNumber: Int, colNumber: Int) : super(msg, lineNumber) {
      initCause(cause)
      this.lineNumber = lineNumber
      this.columnNumber = colNumber
   }

   override fun toString(): String {
      return ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
         .append(this::lineNumber.name, lineNumber)
         .append(this::columnNumber.name, columnNumber)
         .append("errorOffset", errorOffset)
         .append(this::message.name, message)
         .append(this::cause.name, cause)
         .toString()
   }


}
