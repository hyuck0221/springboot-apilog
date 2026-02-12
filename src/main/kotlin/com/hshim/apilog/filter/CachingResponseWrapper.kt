package com.hshim.apilog.filter

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

/**
 * Intercepts all writes to the response output stream/writer, buffering them
 * in memory so the response body can be read after the handler completes.
 *
 * Call [copyBodyToResponse] at the end of the filter chain to flush the
 * buffered content to the actual underlying response.
 */
internal class CachingResponseWrapper(response: HttpServletResponse) : HttpServletResponseWrapper(response) {

    private val buffer = ByteArrayOutputStream()
    private var cachedOutputStream: ServletOutputStream? = null
    private var cachedWriter: PrintWriter? = null

    val cachedBody: ByteArray
        get() {
            cachedWriter?.flush()
            cachedOutputStream?.flush()
            return buffer.toByteArray()
        }

    override fun getOutputStream(): ServletOutputStream {
        if (cachedOutputStream == null) {
            cachedOutputStream = object : ServletOutputStream() {
                override fun write(b: Int) = buffer.write(b)
                override fun write(b: ByteArray) = buffer.write(b)
                override fun write(b: ByteArray, off: Int, len: Int) = buffer.write(b, off, len)
                override fun isReady(): Boolean = true
                override fun setWriteListener(listener: WriteListener?) {}
            }
        }
        return cachedOutputStream!!
    }

    override fun getWriter(): PrintWriter {
        if (cachedWriter == null) {
            cachedWriter = PrintWriter(
                OutputStreamWriter(buffer, characterEncoding ?: "UTF-8"),
                true,
            )
        }
        return cachedWriter!!
    }

    /** Writes the buffered response body to the actual underlying response. */
    fun copyBodyToResponse() {
        cachedWriter?.flush()
        cachedOutputStream?.flush()
        if (buffer.size() > 0) {
            response.outputStream.write(buffer.toByteArray())
            response.outputStream.flush()
        }
    }
}
