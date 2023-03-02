package me.kbai.mfinstaller.tool.xapk

import android.app.Activity
import me.kbai.mfinstaller.tool.AppInstallUtils
import java.io.File

/**
 * @author Sean on 2023/3/2
 */
class SingleXapkInstaller(xapkFile: File, unzipDir: File) : XapkInstaller(xapkFile, unzipDir) {

    override fun install(context: Activity) {
        unzipDir.listFiles()?.forEach { file ->
            if ((file.isFile && file.name.endsWith(".apk"))) {
                AppInstallUtils.installApk(context, xapkFile)
            }
        }
    }

    override fun getUnzipPath() = unzipDir.listFiles()
        ?.find { file -> file.isFile && file.name.endsWith(".apk") }
        ?.absolutePath
        ?: ""
}