package me.kbai.mfinstaller

import android.app.Application
import android.content.Context
import me.kbai.mfinstaller.tool.AppInstallUtils

/**
 * @author sean 2023/3/2
 */
class MfiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppInstallUtils.initAppContext(this)
    }
}