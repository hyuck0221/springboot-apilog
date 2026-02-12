package com.hshim.apilog.autoconfigure

import com.hshim.apilog.config.ApiLogProperties
import com.hshim.apilog.filter.ApiLogFilter
import com.hshim.apilog.storage.ApiLogStorage
import com.hshim.apilog.storage.db.ApiLogDbStorage
import com.hshim.apilog.storage.local.ApiLogLocalFileStorage
import com.hshim.apilog.storage.supabase.ApiLogSupabaseDbStorage
import com.hshim.apilog.storage.supabase.ApiLogSupabaseS3Storage
import com.hshim.apilog.view.controller.ApiLogFileController
import com.hshim.apilog.view.controller.ApiLogViewController
import com.hshim.apilog.view.filter.ApiLogViewAuthFilter
import com.hshim.apilog.view.service.ApiLogFileService
import com.hshim.apilog.view.service.ApiLogViewService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Spring Boot auto-configuration entry point for the apilog library.
 *
 * Activated when:
 * - A web application context is detected.
 * - `apilog.enabled` is `true` (default when not set).
 *
 * DB storage and View API are handled by nested configurations guarded by
 * `@ConditionalOnClass(JdbcTemplate::class)` so they are only evaluated when
 * `spring-jdbc` (or `spring-boot-starter-jdbc`) is present on the classpath.
 */
@AutoConfiguration(after = [JdbcTemplateAutoConfiguration::class])
@ConditionalOnWebApplication
@EnableConfigurationProperties(ApiLogProperties::class)
@ConditionalOnProperty(prefix = "apilog", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ApiLogAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "apilog.storage.local-file", name = ["enabled"], havingValue = "true")
    fun apiLogLocalFileStorage(
        properties: ApiLogProperties,
    ): ApiLogLocalFileStorage = ApiLogLocalFileStorage(properties)

    @Bean
    @ConditionalOnProperty(prefix = "apilog.storage.supabase-db", name = ["enabled"], havingValue = "true")
    fun apiLogSupabaseDbStorage(
        properties: ApiLogProperties,
    ): ApiLogSupabaseDbStorage = ApiLogSupabaseDbStorage(properties)

    @Bean
    @ConditionalOnProperty(prefix = "apilog.storage.supabase-s3", name = ["enabled"], havingValue = "true")
    fun apiLogSupabaseS3Storage(
        properties: ApiLogProperties,
    ): ApiLogSupabaseS3Storage = ApiLogSupabaseS3Storage(properties)

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

    /**
     * Sub-configuration for the DB storage backend.
     *
     * Guarded by `@ConditionalOnClass(JdbcTemplate::class)` so the class is only loaded
     * when `spring-jdbc` is present on the classpath, preventing [ClassNotFoundException].
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JdbcTemplate::class)
    @ConditionalOnProperty(prefix = "apilog.storage.db", name = ["enabled"], havingValue = "true")
    class DbStorageConfiguration {

        @Bean
        @ConditionalOnBean(JdbcTemplate::class)
        fun apiLogDbStorage(
            jdbcTemplate: JdbcTemplate,
            properties: ApiLogProperties,
        ): ApiLogDbStorage = ApiLogDbStorage(jdbcTemplate, properties)
    }

    /**
     * Sub-configuration for the built-in View API (log ingestion + query endpoints).
     *
     * Activated when ALL of the following are true:
     * - `spring-jdbc` (JdbcTemplate) is on the classpath
     * - A `JdbcTemplate` bean exists (a `DataSource` is configured)
     * - `apilog.view.enabled=true`
     *
     * **Optional API key auth:**
     * Set `apilog.view.api-key` to a non-blank value to require `X-Api-Key: <key>`
     * on every request to the View API. Omit or leave blank to allow open access.
     *
     * **Endpoints:**
     * ```
     * POST {basePath}/logs/receive       Ingest a log entry from a remote application
     * GET  {basePath}/logs               Paginated & filtered log list
     * GET  {basePath}/logs/{id}          Single log entry detail
     * GET  {basePath}/logs/stats         Aggregated statistics
     * GET  {basePath}/logs/apps          Distinct application names
     * ```
     */
    /**
     * Sub-configuration for the file browsing API endpoints.
     *
     * Activated when `apilog.view.enabled=true`. Does NOT require a DataSource,
     * so it works even when only local-file storage is used.
     *
     * Endpoints:
     * ```
     * GET {basePath}/files           List log files in a directory
     * GET {basePath}/files/content   Return raw text content of a file
     * ```
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "apilog.view", name = ["enabled"], havingValue = "true")
    class FileConfiguration {

        @Bean
        fun apiLogFileService(): ApiLogFileService = ApiLogFileService()

        @Bean
        fun apiLogFileController(fileService: ApiLogFileService): ApiLogFileController =
            ApiLogFileController(fileService)
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JdbcTemplate::class)
    @ConditionalOnProperty(prefix = "apilog.view", name = ["enabled"], havingValue = "true")
    class ViewConfiguration {

        @Bean
        @ConditionalOnBean(JdbcTemplate::class)
        fun apiLogViewService(
            jdbcTemplate: JdbcTemplate,
            properties: ApiLogProperties,
        ): ApiLogViewService = ApiLogViewService(jdbcTemplate, properties)

        @Bean
        @ConditionalOnBean(ApiLogViewService::class)
        fun apiLogViewController(
            viewService: ApiLogViewService,
            storagesProvider: ObjectProvider<ApiLogStorage>,
        ): ApiLogViewController = ApiLogViewController(viewService, storagesProvider)

        /**
         * Registers [ApiLogViewAuthFilter] only when `apilog.view.api-key` is non-blank.
         * The filter is scoped exclusively to `{basePath}/**` so it does not affect
         * any other endpoints in the application.
         */*/
        @Bean
        @ConditionalOnProperty(prefix = "apilog.view", name = ["api-key"], matchIfMissing = false)
        fun apiLogViewAuthFilterRegistration(
            properties: ApiLogProperties,
        ): FilterRegistrationBean<ApiLogViewAuthFilter> =
            FilterRegistrationBean(ApiLogViewAuthFilter(properties)).apply {
                order = Ordered.HIGHEST_PRECEDENCE
                addUrlPatterns("${properties.view.basePath}/*")
            }
    }
}
