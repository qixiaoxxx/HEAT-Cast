package com.mcast.heat.manager

import android.content.Context
import android.os.Environment
import android.util.Log
import com.mcast.heat.util.FileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit


object Download {

    private val downloadService = DownloadService.create()

    var isDownloading = false

    fun downloadFile(url: String, context: Context) =
        callbackFlow {
            isDownloading = true
            try {
                val filename = url.substringAfterLast("/")
                val filePath = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    filename
                ).absolutePath
                var tempDownloadSize = 0L
                Log.i(TAG, "downloadFile: $filePath")
                val responseBody = downloadService.download(url)
                Log.i(TAG, "responseBody: ${responseBody.contentLength()}")

                responseBody.use { body ->
                    val contentLength = body.contentLength()
                    Log.i(TAG, "contentLength: $contentLength")
                    val input = body.byteStream()
                    BufferedOutputStream(FileOutputStream(filePath, false)).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead = 0L
                        var nanoStartTime = System.nanoTime()
                        while (true) {
                            val len = input.read(buffer)
                            val nanoStopTime = System.nanoTime()

                            if (len == -1) {
                                trySend(
                                    Progress.obtain().apply {
                                        this.bytesRead = FileSize.getFileSize(bytesRead)
                                        this.totalSize = FileSize.getFileSize(contentLength)
                                        this.isDone = true
                                        this.progress = 100
                                        this.filePath = filePath
                                    },
                                )
                                break
                            }
                            bytesRead += len
                            Log.i(TAG, "bytesRead: $bytesRead")
                            if (hasIntervalTimeElapsed(
                                    nanoStartTime,
                                    nanoStopTime,
                                )
                            ) {
                                val size = bytesRead - tempDownloadSize
                                trySend(
                                    Progress.obtain().apply {
                                        this.bytesRead = FileSize.getFileSize(bytesRead)
                                        this.totalSize = FileSize.getFileSize(contentLength)
                                        this.isDone = false
                                        this.progress = (bytesRead * 100 / contentLength).toInt()
                                        this.perSecondBytes =
                                            FileSize.getFileSize(size * 1_000_000_000L / (nanoStopTime - nanoStartTime)) + "/s"
                                    },
                                )
                                Log.i(TAG, "bytesRead: $bytesRead")
                                tempDownloadSize = bytesRead
                                nanoStartTime = nanoStopTime
                            }

                            output.write(buffer, 0, len)
                            yield()
                        }
                    }
                }
            } catch (e: IOException) {
                Log.i(TAG, "download  SocketException${e.javaClass}: ${e.message}")
                trySend(
                    Progress.obtain().apply {
                        downloadError = true
                        needRetry = true
                    },
                )
                cancel()
            } catch (e: Exception) {
                Log.i(TAG, "download  Exception${e.javaClass}: ${e.message}")
                trySend(
                    Progress.obtain().apply {
                        downloadError = true
                    },
                )
                cancel()
            } finally {
                channel.close()
            }

            awaitClose {
                isDownloading = false
            }
        }.flowOn(Dispatchers.IO)

    private fun hasIntervalTimeElapsed(
        nanoStartTime: Long,
        nanoStopTime: Long,
        progressIntervalMilliseconds: Long = 1000L,
    ): Boolean {
        return TimeUnit.NANOSECONDS
            .toMillis(nanoStopTime - nanoStartTime) >= progressIntervalMilliseconds
    }

    const val TAG = "Download"
}


class Progress private constructor() {
    var bytesRead: String = ""
    var totalSize: String = ""
    var perSecondBytes: String = ""
    var progress = 0
    var isDone: Boolean = false
    var filePath = ""
    private var next: Progress? = null
    var downloadError = false
    var needRetry = false

    fun recycle() {
        synchronized(sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool
                sPool = this
                sPoolSize++
            }
        }
    }

    companion object {
        private val sPoolSync = Any()
        private var sPool: Progress? = null
        private var sPoolSize = 0
        const val MAX_POOL_SIZE = 50
        fun obtain(): Progress {
            synchronized(sPoolSync) {
                if (sPool != null) {
                    val m: Progress = sPool!!
                    sPool = m.next
                    m.next = null
                    sPoolSize--
                    return m
                }
            }
            return Progress()
        }
    }
}