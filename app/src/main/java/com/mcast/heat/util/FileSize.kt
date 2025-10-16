package com.mcast.heat.util

import android.annotation.SuppressLint


object FileSize {
    private const val BYTE = 1L
    private const val KILOBYTE = 1024L * BYTE
    private const val MEGABYTE = 1024L * KILOBYTE
    private const val GIGABYTE = 1024L * MEGABYTE
    private const val TERABYTE = 1024L * GIGABYTE

    @SuppressLint("DefaultLocale")
    fun getFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes == 0L -> "0.0B"
            sizeInBytes < KILOBYTE -> String.format("%.1fB", sizeInBytes.toDouble())
            sizeInBytes < MEGABYTE -> String.format("%.1fK", sizeInBytes.toDouble() / KILOBYTE)
            sizeInBytes < GIGABYTE -> String.format("%.1fM", sizeInBytes.toDouble() / MEGABYTE)
            sizeInBytes < TERABYTE -> String.format("%.1fG", sizeInBytes.toDouble() / GIGABYTE)
            else -> String.format("%.1fT", sizeInBytes.toDouble() / TERABYTE)
        }
    }
}