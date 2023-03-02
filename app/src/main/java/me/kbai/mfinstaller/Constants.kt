package me.kbai.mfinstaller

import android.content.Context
import java.io.File

/**
 * @author Sean on 2023/3/2
 */
object Constants {
    var filePath: String = ""
        private set

    var cachePath: String = ""
        private set

    val xapkUnzipPath: String
        get() = cachePath + File.separator + "xapkUnzip"

    fun initPaths(context: Context) {
        if (filePath.isNotBlank()) return
        filePath = context.getExternalFilesDir(null)?.absolutePath ?: ""
        cachePath = context.externalCacheDir?.absolutePath ?: ""
    }
}