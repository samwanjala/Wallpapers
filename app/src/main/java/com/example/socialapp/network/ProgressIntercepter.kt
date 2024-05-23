package com.example.socialapp.network

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException

class ProgressInterceptor(private val listener: ProgressListener) : Interceptor {

    interface ProgressListener {
        fun onProgress(progress: Int)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        return originalResponse.newBuilder()
            .body(ProgressResponseBody(originalResponse.body, listener))
            .build()
    }

    private class ProgressResponseBody(
        private val responseBody: ResponseBody?,
        private val listener: ProgressListener
    ) : ResponseBody() {

        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? {
            return responseBody?.contentType()
        }

        override fun contentLength(): Long {
            return responseBody?.contentLength() ?: -1
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = source(responseBody?.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun source(source: Source?): Source {
            return object : ForwardingSource(source!!) {
                private var totalBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    val fullLength = responseBody?.contentLength() ?: 0
                    if (bytesRead == -1L) {
                        totalBytesRead = fullLength
                    } else {
                        totalBytesRead += bytesRead
                    }
                    val progress = (100 * totalBytesRead / fullLength).toInt()
                    listener.onProgress(progress)
                    return bytesRead
                }
            }
        }
    }
}