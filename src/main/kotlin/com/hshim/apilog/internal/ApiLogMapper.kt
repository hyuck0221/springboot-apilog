package com.hshim.apilog.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Library-internal JSON mapper singleton.
 *
 * Kept separate from the consumer application's own [ObjectMapper] bean so that
 * apilog serialization behaviour is not affected by the consumer's Jackson configuration.
 */
internal object ApiLogMapper {

    private val mapper: ObjectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()

    fun toJson(value: Any): String = mapper.writeValueAsString(value)

    fun <T> fromJson(json: String, type: Class<T>): T = mapper.readValue(json, type)
}
