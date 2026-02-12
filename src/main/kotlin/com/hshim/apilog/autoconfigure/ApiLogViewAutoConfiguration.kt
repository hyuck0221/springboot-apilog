package com.hshim.apilog.autoconfigure

import com.hshim.apilog.config.ApiLogProperties
import com.hshim.apilog.storage.ApiLogStorage
import com.hshim.apilog.view.controller.ApiLogViewController
import com.hshim.apilog.view.service.ApiLogViewService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper

/**
 * Auto-configuration for the built-in View API (log ingestion + query endpoints).
 *
 * Activated when ALL of the following are true:
 * - `spring-jdbc` (JdbcTemplate) is on the classpath
 * - A `JdbcTemplate` bean exists (i.e., a `DataSource` is configured)
 * - `apilog.view.enabled=true`
 *
 * Registers:
 * - [ApiLogViewService] — queries log entries from the DB
 * - [ApiLogViewController] — REST endpoints at `apilog.view.base-path` (default: `/apilog`)
 *
 * **Endpoints provided:**
 * ```
 * POST {basePath}/logs/receive       Ingest a log entry from a remote application
 * GET  {basePath}/logs               Paginated & filtered log list
 * GET  {basePath}/logs/{id}          Single log entry detail
 * GET  {basePath}/logs/stats         Aggregated statistics
 * GET  {basePath}/logs/apps          Distinct application names
 * ```
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JdbcTemplate::class)
@ConditionalOnProperty(prefix = "apilog.view", name = ["enabled"], havingValue = "true")
class ApiLogViewAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate::class)
    fun apiLogViewService(
        jdbcTemplate: JdbcTemplate,
        properties: ApiLogProperties,
        objectMapper: ObjectMapper,
    ): ApiLogViewService = ApiLogViewService(jdbcTemplate, properties, objectMapper)

    @Bean
    @ConditionalOnBean(ApiLogViewService::class)
    fun apiLogViewController(
        viewService: ApiLogViewService,
        storagesProvider: ObjectProvider<ApiLogStorage>,
    ): ApiLogViewController = ApiLogViewController(viewService, storagesProvider)
}
