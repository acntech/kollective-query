package no.acntech.kollectiveq.test.app.util.json

import com.fasterxml.jackson.databind.ObjectMapper

inline fun <reified T> ObjectMapper.fromJson(json: String): T =
   this.readValue(json, T::class.java)