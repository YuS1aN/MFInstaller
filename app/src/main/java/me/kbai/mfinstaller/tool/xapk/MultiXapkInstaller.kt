package me.kbai.mfinstaller.tool.xapk

import android.app.Activity
import android.content.Intent
import me.kbai.mfinstaller.ui.InstallActivity
import java.io.File

/**
 * @author Sean on 2023/3/2
 */
class MultiXapkInstaller(xapkFile: File, unzipDir: File) : XapkInstaller(xapkFile, unzipDir) {

    override fun install(context: Activity) {
        val apkFilePaths = unzipDir.listFiles()!!
            .filter { file -> file.isFile && file.name.endsWith(".apk") }
            .map { it.absolutePath }
            .toCollection(ArrayList())
        startInstallActivity(xapkFile.absolutePath, apkFilePaths, context)
    }

    override fun getUnzipPath(): String = unzipDir.absolutePath

    private fun startInstallActivity(
        xapkPath: String,
        apkFilePaths: ArrayList<String>,
        context: Activity
    ) {
        val intent = Intent(context, InstallActivity::class.java)
            .putExtra(InstallActivity.KEY_XAPK_PATH, xapkPath)
            .putStringArrayListExtra(InstallActivity.KEY_APK_PATHS, apkFilePaths)
        context.startActivityForResult(intent, InstallActivity.INSTALL_REQUEST_CODE)
    }
}