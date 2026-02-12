package com.hshim.apilog.storage

import com.hshim.apilog.model.ApiLogEntry

/**
 * Strategy interface for persisting [ApiLogEntry] records.
 *
 * Implement this interface to add a custom storage backend.
 * Multiple implementations can be active simultaneously — each will receive every log entry.
 */
interface ApiLogStorage {
    fun save(entry: ApiLogEntry)
}
