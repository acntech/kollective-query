package no.acntech.kollectiveq.lang

/**
 * Thrown to indicate a validation problem.
 */
class ValidationException : RuntimeException {

   constructor() : super()

   constructor(message: String) : super(message)

   constructor(message: String, cause: Throwable) : super(message, cause)

   constructor(cause: Throwable) : super(cause)
}
