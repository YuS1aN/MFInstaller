package me.kbai.mfinstaller.tool.xapk

import android.app.Activity
import net.lingala.zip4j.exception.ZipException
import java.io.File

/**
 * @author Sean on 2023/3/2
 */
abstract class XapkInstaller(val xapkFile: File, val unzipDir: File) {

    fun installXapk(context: Activity) = try {
        install(context)
    } catch (e: ZipException) {
        e.printStackTrace()
    }

    abstract fun install(context: Activity)

    abstract fun getUnzipPath(): String
}