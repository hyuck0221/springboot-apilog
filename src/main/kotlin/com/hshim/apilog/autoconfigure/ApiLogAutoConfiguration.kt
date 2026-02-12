package com.hshim.apilog.autoconfigure

import com.hshim.apilog.config.ApiLogProperties
import com.hshim.apilog.filter.ApiLogFilter
import com.hshim.apilog.storage.ApiLogStorage
import com.hshim.apilog.storage.http.ApiLogHttpStorage
import com.hshim.apilog.storage.local.ApiLogLocalFileStorage
import com.hshim.apilog.storage.supabase.ApiLogSupabaseDbStorage
import com.hshim.apilog.storage.supabase.ApiLogSupabaseS3Storage
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import tools.jackson.databind.ObjectMapper

/**
 * Spring Boot auto-configuration entry point for the apilog library.
 *
 * Activated when:
 * - A web application context is detected.
 * - `apilog.enabled` is `true` (default when not set).
 *
 * Sub-configurations imported via `@Import`:
 * - [ApiLogDbAutoConfiguration] — DB storage (guarded by `@ConditionalOnClass(JdbcTemplate)`)
 * - [ApiLogViewAutoConfiguration] — View API (guarded by `apilog.view.enabled=true` + JDBC)
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(ApiLogProperties::class)
@ConditionalOnProperty(prefix = "apilog", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@Import(ApiLogDbAutoConfiguration::class, ApiLogViewAutoConfiguration::class)
class ApiLogAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "apilog.storage.local-file", name = ["enabled"], havingValue = "true")
    fun apiLogLocalFileStorage(
        properties: ApiLogProperties,
        objectMapper: ObjectMapper,
    ): ApiLogLocalFileStorage = ApiLogLocalFileStorage(properties, objectMapper)

    @Bean
    @ConditionalOnProperty(prefix = "apilog.storage.supabase-db", name = ["enabled"], havingValue = "true")
    fun apiLogSupabaseDbStorage(
        properties: ApiLogProperties,
        objectMapper: ObjectMapper,
    ): ApiLogSupabaseDbStorage = ApiLogSupabaseDbStorage(properties, objectMapper)

    @Bean
    @ConditionalOnProperty(prefix = "apilog.storage.supabase-s3", name = ["enabled"], havingValue = "true")
    fun apiLogSupabaseS3Storage(
        properties: ApiLogProperties,
        objectMapper: ObjectMapper,
    ): ApiLogSupabaseS3Storage = ApiLogSupabaseS3Storage(properties, objectMapper)

    @Bean
    @ConditionalOnProperty(prefix = "apilog.storage.http", name = ["enabled"], havingValue = "true")
    fun apiLogHttpStorage(
        properties: ApiLogProperties,
        objectMapper: ObjectMapper,
    ): ApiLogHttpStorage = ApiLogHttpStorage(properties, objectMapper)

    /**
     * The main filter bean. All [ApiLogStorage] beans are collected via [ObjectProvider]
     * so the filter remains functional even when no storage backend is configured.
     */
    @Bean
    fun apiLogFilter(
        properties: ApiLogProperties,
        storagesProvider: ObjectProvider<ApiLogStorage>,
    ): ApiLogFilter = ApiLogFilter(properties, storagesProvider.orderedStream().toList())

    /**
     * Registers [ApiLogFilter] with near-lowest precedence so it wraps the entire
     * request lifecycle. Adjust the order if needed via your own [FilterRegistrationBean].
     */
    @Bean
    fun apiLogFilterRegistration(apiLogFilter: ApiLogFilter): FilterRegistrationBean<ApiLogFilter> =
        FilterRegistrationBean(apiLogFilter).apply {
            order = Ordered.LOWEST_PRECEDENCE - 10
            addUrlPatterns("/*")
        }
}
