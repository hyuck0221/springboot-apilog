package com.hshim.apilog.autoconfigure

import com.hshim.apilog.config.ApiLogProperties
import com.hshim.apilog.storage.db.ApiLogDbStorage
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper

/**
 * Sub-configuration for the DB storage backend.
 *
 * Guarded by `@ConditionalOnClass(JdbcTemplate::class)` so it is only evaluated when
 * `spring-jdbc` (or `spring-boot-starter-jdbc`) is present on the consumer's classpath.
 * The `@ConditionalOnBean` further ensures that a `JdbcTemplate` bean actually exists.
 *
 * Consumer applications must add `spring-boot-starter-jdbc` to their build and configure
 * a `DataSource` for this storage to activate.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JdbcTemplate::class)
@ConditionalOnProperty(prefix = "apilog.storage.db", name = ["enabled"], havingValue = "true")
class ApiLogDbAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate::class)
    fun apiLogDbStorage(
        jdbcTemplate: JdbcTemplate,
        properties: ApiLogProperties,
        objectMapper: ObjectMapper,
    ): ApiLogDbStorage = ApiLogDbStorage(jdbcTemplate, properties, objectMapper)
}
