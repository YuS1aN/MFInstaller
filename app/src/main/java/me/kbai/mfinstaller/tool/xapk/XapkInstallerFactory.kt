package me.kbai.mfinstaller.tool.xapk

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Environment
import me.kbai.mfinstaller.Constants
import me.kbai.mfinstaller.tool.ObbAccessUtils
import me.kbai.mfinstaller.tool.Zip4JUtils
import net.lingala.zip4j.exception.ZipException
import java.io.File

/**
 * @author Sean on 2023/3/2
 */
object XapkInstallerFactory {

    fun getXapkIcon(xapkFile: File): File? {
        val unzipDir = createUnzipOutputDir(xapkFile) ?: return null
        val files = unzipDir.listFiles()
        if (files != null && files.isNotEmpty()) {
            files.forEach { file ->
                if ((file.isFile && file.name.endsWith(".png"))) {
                    return file
                }
            }
        }
        try {
            Zip4JUtils.unzip(xapkFile, unzipDir.absolutePath) { name ->
                when {
                    name.endsWith(".png") -> true
                    else -> false
                }
            }
        } catch (e: ZipException) {
            e.printStackTrace()
            return null
        }
        unzipDir.listFiles()?.forEach { file ->
            if ((file.isFile && file.name.endsWith(".png"))) {
                return file
            }
        }
        return null
    }

    private fun createUnzipOutputDir(file: File): File? {
        val unzipOutputDir = Constants.xapkUnzipPath + File.separator + file.nameWithoutExtension
        val unzipDirFile = File(unzipOutputDir)
        val dir = unzipDirFile.run { exists() && isDirectory || mkdirs() }
        return if (dir) unzipDirFile else null
    }

    @JvmStatic
    fun createXapkInstaller(context: Context, xapkFile: File): XapkInstaller? {
        val unzipDir = createUnzipOutputDir(xapkFile) ?: return null
        try {
            Zip4JUtils.unzip(xapkFile, unzipDir.absolutePath) { name ->
                when {
                    name.endsWith(".apk") -> true
                    name.endsWith(".png") -> true
                    else -> false
                }
            }
        } catch (e: ZipException) {
            e.printStackTrace()
            return null
        }
        val apkSize = unzipDir.listFiles()!!
            .count { file -> file.isFile && file.name.endsWith(".apk") }

        if (!installObb(context, xapkFile)) return null

        return if (apkSize > 1) {
            MultiXapkInstaller(xapkFile, unzipDir)
        } else {
            SingleXapkInstaller(xapkFile, unzipDir)
        }
    }


    private fun installObb(context: Context, xapkFile: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return installObbApi30(context, xapkFile)
        }
        return installObbDirect(xapkFile)
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun installObbApi30(context: Context, xapkFile: File): Boolean {
        val unzipDir = createUnzipOutputDir(xapkFile) ?: return false
        val unzipPath = unzipDir.absolutePath
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath
        val paths = ArrayList<String>()

        try {
            Zip4JUtils.unzip(xapkFile, unzipPath) { path ->
                when {
                    path.startsWith("Android/obb") -> {
                        paths.add(path)
                        true
                    }
                    else -> false
                }
            }
            paths.forEach {
                val src = unzipPath + File.separator + it
                val dest = storageRoot + File.separator + it
                if (!ObbAccessUtils.copyBySAF(context, src, dest)) {
                    return false
                }
            }
            return true
        } catch (e: ZipException) {
            e.printStackTrace()
            return false
        }
    }

    private fun installObbDirect(xapkFile: File): Boolean {
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath
        return try {
            Zip4JUtils.unzip(xapkFile, storageRoot) { name ->
                when {
                    name.startsWith("Android/obb") -> true
                    else -> false
                }
            }
            true
        } catch (e: ZipException) {
            e.printStackTrace()
            false
        }
    }
}