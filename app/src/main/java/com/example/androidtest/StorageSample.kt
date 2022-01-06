package com.example.androidtest

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File


object StorageSample {
    fun external(context: Context) {

        context.getExternalFilesDir(null)

        val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    }

    private fun getAvailableSize(path: File): Long {
        if (!path.exists()) {
            return -1
        }
        return try {
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            availableBlocks * blockSize
        } catch (e: Exception) {
            -1
        }
    }

    private fun getTotalSize(path: File): Long {
        if (!path.exists()) {
            return -1
        }
        return try {
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.blockCountLong
            availableBlocks * blockSize
        } catch (e: Exception) {
            -1
        }
    }

    fun getAvailableInternalMemorySize(): Long {
        return getAvailableSize(Environment.getDataDirectory())
    }

    fun getTotalInternalMemorySize(): Long {
        return getTotalSize(Environment.getDataDirectory())
    }

    fun getAvailableExternalMemorySize(): Long {
        return getAvailableSize(Environment.getExternalStorageDirectory())
    }

    fun getTotalExternalMemorySize(): Long {
        return getTotalSize(Environment.getExternalStorageDirectory())
    }
}